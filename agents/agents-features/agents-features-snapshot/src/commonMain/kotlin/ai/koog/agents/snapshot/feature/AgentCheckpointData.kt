@file:OptIn(InternalAgentsApi::class)

package ai.koog.agents.snapshot.feature

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.AgentContextData
import ai.koog.agents.core.agent.context.GraphAgentContextData
import ai.koog.agents.core.agent.context.PlannerAgentContextData
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.planner.PlannerAgentExecutionPoint
import ai.koog.agents.snapshot.providers.PersistenceUtils
import ai.koog.prompt.message.Message
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONNull
import ai.koog.serialization.JSONObject
import ai.koog.serialization.JSONPrimitive
import ai.koog.serialization.JSONSerializer
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents the checkpoint data for an agent's state during a session.
 *
 * @property checkpointId The unique identifier of the checkpoint. This allows tracking and restoring the agent's session to a specific state.
 * @property messageHistory A list of messages exchanged in the session up to the checkpoint. Messages include interactions between the user, system, assistant, and tools.
 * @property properties Additional data associated with the checkpoint. This can be used to store additional information about the agent's state.
 * @property createdAt The timestamp when the checkpoint was created.
 * @property version The version of the checkpoint data structure
 */
@Serializable
public data class AgentCheckpointData(
    val checkpointId: String,
    val createdAt: Instant,
    val messageHistory: List<Message>,
    val version: Long,
    val properties: JSONObject
)

/**
 * Creates a tombstone checkpoint for an agent's session.
 */
@OptIn(ExperimentalUuidApi::class)
public fun tombstoneCheckpoint(
    createdAt: Instant,
    version: Long,
): AgentCheckpointData {
    return AgentCheckpointData(
        checkpointId = Uuid.random().toString(),
        createdAt = createdAt,
        messageHistory = emptyList(),
        version = version,
        properties = JSONObject(
            mapOf(
                PersistenceUtils.TOMBSTONE_CHECKPOINT_NAME to JSONPrimitive(true)
            )
        )
    )
}

/**
 * Specialized data for graph-based agents, including execution path and input/output states.
 *
 * @property nodePath The identifier of the node where the checkpoint was created.
 * @property lastInput Deprecated. Serialized input received for the node with [nodePath].
 * @property lastOutput Serialized output received from the node with [nodePath].
 */
@Serializable
public data class GraphCheckpointProperties(
    public val nodePath: String,
    @Deprecated("Use lastOutput instead, lastOutput will be removed in future versions")
    public val lastInput: JSONElement = JSONNull,
    public val lastOutput: JSONElement = JSONNull
) {
    init {
        require(lastInput == JSONNull || lastOutput == JSONNull) { "`lastInput` and `lastOutput` cannot be both set" }
        require(lastInput != JSONNull || lastOutput != JSONNull) { "`lastInput` (until 0.6.0) or `lastOutput` (since 0.6.1) must be set" }
    }
}

/**
 * Specialized data for planner agents, capturing state, plan, and current execution point.
 *
 * @property executionPoint The current point in the planner's execution cycle.
 * @property state Serialized state of the planner agent.
 * @property plan Serialized current plan.
 */
@Serializable
public data class PlannerCheckpointProperties(
    public val executionPoint: PlannerAgentExecutionPoint,
    public val state: JSONElement,
    public val plan: JSONElement
)

/**
 * Converts an instance of [AgentCheckpointData] to [AgentContextData].
 */
@InternalAgentsApi
public fun AgentCheckpointData.toAgentContextData(
    rollbackStrategy: RollbackStrategy,
    serializer: JSONSerializer,
    additionalRollbackActions: suspend (AIAgentContext) -> Unit = {}
): AgentContextData? {
    runCatching {
        serializer.decodeFromJSONElement<GraphCheckpointProperties>(
            properties,
            typeToken<GraphCheckpointProperties>()
        )
    }.getOrNull()?.let { graphProperties ->
        return GraphAgentContextData(
            messageHistory = messageHistory,
            nodePath = graphProperties.nodePath,
            lastInput = graphProperties.lastInput,
            lastOutput = graphProperties.lastOutput,
            rollbackStrategy = rollbackStrategy,
            additionalRollbackActions = additionalRollbackActions
        )
    }

    runCatching {
        serializer.decodeFromJSONElement<PlannerCheckpointProperties>(
            properties,
            typeToken<PlannerCheckpointProperties>()
        )
    }.getOrNull()?.let { plannerProperties ->
        return PlannerAgentContextData(
            messageHistory = messageHistory,
            state = plannerProperties.state,
            plan = plannerProperties.plan,
            executionPoint = plannerProperties.executionPoint,
            rollbackStrategy = rollbackStrategy,
            additionalRollbackActions = additionalRollbackActions
        )
    }

    return null
}

/**
 * Checks whether the `AgentCheckpointData` instance is marked as a tombstone.
 */
public fun AgentCheckpointData.isTombstone(): Boolean =
    properties.entries[PersistenceUtils.TOMBSTONE_CHECKPOINT_NAME] == JSONPrimitive(true)

package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import ai.koog.prompt.structure.json.JsonStructure
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Common logic for ClaudeAgentBuilder.
 */
public abstract class ClaudeAgentBuilderCommon<Self : ClaudeAgentBuilderCommon<Self>> internal constructor(
    transport: CliTransport,
    systemPrompt: String?,
    llModel: LLModel?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String? = null,
    permissionMode: ClaudePermissionMode? = null,
    additionalFlags: List<String> = emptyList(),
) : ClaudeAgentBuilderBase<String, CliAIAgentResponse, Self>(
    transport, systemPrompt, llModel, workspace, timeout, id, clock, featureInstallers, apiKey, permissionMode, additionalFlags
) {
    @OptIn(InternalSerializationApi::class)
    public fun <Output : Any> structure(
        outputClass: KClass<Output>
    ): ClaudeAgentStructuredOutputBuilder<Output> {
        return structure(JsonStructure.create(serializer = outputClass.serializer()))
    }

    public fun <Output> structure(
        structure: Structure<Output, LLMParams.Schema.JSON>
    ): ClaudeAgentStructuredOutputBuilder<Output> = ClaudeAgentStructuredOutputBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers,
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        structure = structure
    )

    public fun <Input> generateRequest(
        generateRequest: CliConfig.GenerateRequest<Input>
    ): ClaudeAgentGenericInputBuilder<Input> = ClaudeAgentGenericInputBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers,
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        generateRequest = generateRequest
    )

    public fun build(): CliAIAgent<String, CliAIAgentResponse> {
        val finalTransport = requireNotNull(this.transport) { "Transport is required" }
        return CliAIAgent.claude(
            transport = finalTransport,
            apiKey = apiKey,
            systemPrompt = null,
            llModel = null,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            id = id,
            clock = clock,
            installFeatures = { featureInstallers.forEach { it(this) } }
        )
    }
}

/**
 * Common logic for ClaudeAgentGenericInputBuilder.
 */
public abstract class ClaudeAgentGenericInputBuilderCommon<Input, Self : ClaudeAgentGenericInputBuilderCommon<Input, Self>> internal constructor(
    transport: CliTransport,
    systemPrompt: String?,
    llModel: LLModel?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    internal val generateRequest: CliConfig.GenerateRequest<Input>,
) : ClaudeAgentBuilderBase<Input, CliAIAgentResponse, Self>(
    transport, systemPrompt, llModel, workspace, timeout, id, clock, featureInstallers, apiKey, permissionMode, additionalFlags
) {
    @OptIn(InternalSerializationApi::class)
    public fun <Output : Any> structure(
        outputClass: KClass<Output>
    ): ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output> {
        return structure(JsonStructure.create(serializer = outputClass.serializer()))
    }

    public fun <Output> structure(
        structure: Structure<Output, LLMParams.Schema.JSON>
    ): ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output> = ClaudeAgentGenericInputStructuredOutputBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers,
        apiKey = apiKey,
        permissionMode = permissionMode,
        additionalFlags = additionalFlags,
        generateRequest = generateRequest,
        structure = structure
    )

    public fun build(): CliAIAgent<Input, CliAIAgentResponse> {
        return CliAIAgent.claude<Input>(
            transport = transport,
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            llModel = llModel,
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            id = id,
            clock = clock,
            generateRequest = generateRequest,
            installFeatures = { featureInstallers.forEach { it(this) } }
        )
    }
}

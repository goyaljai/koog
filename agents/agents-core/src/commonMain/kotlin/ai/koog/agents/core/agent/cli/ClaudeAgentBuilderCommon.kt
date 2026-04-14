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
    /**
     * Configures the agent to produce structured output using the specified output class.
     *
     * @param Output The type of the structured output.
     * @param outputClass The Kotlin class representing the output structure.
     * @return Builder for Claude CLI agent with structured output.
     */
    @OptIn(InternalSerializationApi::class)
    public fun <Output : Any> structure(
        outputClass: KClass<Output>
    ): ClaudeAgentStructuredOutputBuilder<Output> {
        return structure(JsonStructure.create(serializer = outputClass.serializer()))
    }

    /**
     * Configures the agent to produce structured output using the specified structure definition.
     *
     * @param Output The type of the structured output.
     * @param structure The structure definition for parsing the output.
     * @return Builder for Claude CLI agent with structured output.
     */
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

    /**
     * Configures a custom request generator for the agent.
     *
     * @param Input The type of the input data.
     * @param generateRequest Function to generate the request string from the input.
     * @return Builder for Claude CLI agent with custom input type.
     */
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

    /**
     * Builds the Claude CLI agent.
     *
     * @return A configured [CliAIAgent] instance that accepts String input and produces [CliAIAgentResponse].
     */
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
    /**
     * Configures the agent to produce structured output using the specified output class.
     *
     * @param Output The type of the structured output.
     * @param outputClass The Kotlin class representing the output structure.
     * @return Builder for Claude CLI agent with custom input type and structured output.
     */
    @OptIn(InternalSerializationApi::class)
    public fun <Output : Any> structure(
        outputClass: KClass<Output>
    ): ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output> {
        return structure(JsonStructure.create(serializer = outputClass.serializer()))
    }

    /**
     * Configures the agent to produce structured output using the specified structure definition.
     *
     * @param Output The type of the structured output.
     * @param structure The structure definition for parsing the output.
     * @return Builder for Claude CLI agent with custom input type and structured output.
     */
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

    /**
     * Builds the Claude CLI agent with custom input type.
     *
     * @return A configured [CliAIAgent] instance that accepts custom input and produces [CliAIAgentResponse].
     */
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

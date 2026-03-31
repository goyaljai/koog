package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Default builder for Claude CLI agent.
 */
public expect class ClaudeAgentBuilder internal constructor(
    config: AIAgentConfig,
    transport: CliTransport?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String? = null,
    permissionMode: ClaudePermissionMode? = null,
    additionalFlags: List<String> = emptyList(),
) : ClaudeAgentBuilderCommon<ClaudeAgentBuilder> {
    override fun self(): ClaudeAgentBuilder
}

/**
 * Builder for Claude CLI agent with custom input type.
 */
public expect class ClaudeAgentGenericInputBuilder<Input> internal constructor(
    config: AIAgentConfig,
    transport: CliTransport?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    generateRequest: CliConfig.GenerateRequest<Input>,
) : ClaudeAgentGenericInputBuilderCommon<Input, ClaudeAgentGenericInputBuilder<Input>> {
    override fun self(): ClaudeAgentGenericInputBuilder<Input>
}

/**
 * Builder for Claude CLI agent with structured output.
 */
public class ClaudeAgentStructuredOutputBuilder<Output> internal constructor(
    config: AIAgentConfig,
    transport: CliTransport?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    internal val structure: Structure<Output, LLMParams.Schema.JSON>,
) : ClaudeAgentBuilderBase<String, CliAgentStructuredResponse<Output>, ClaudeAgentStructuredOutputBuilder<Output>>(
    config, transport, workspace, timeout, id, clock, featureInstallers, apiKey, permissionMode, additionalFlags
) {
    override fun self(): ClaudeAgentStructuredOutputBuilder<Output> = this

    public fun <Input> generateRequest(
        generateRequest: CliConfig.GenerateRequest<Input>
    ): ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output> = ClaudeAgentGenericInputStructuredOutputBuilder(
        config = config,
        transport = transport,
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

    public fun build(): CliAIAgent<String, CliAgentStructuredResponse<Output>> {
        val finalTransport = requireNotNull(this.transport) { "Transport is required" }
        return CliAIAgent.claude(
            transport = finalTransport,
            apiKey = apiKey,
            structure = structure,
            systemPrompt = null, // systemPrompt is already in AIAgentConfig
            llModel = null, // llModel is already in AIAgentConfig
            permissionMode = permissionMode,
            additionalFlags = additionalFlags,
            workspace = workspace,
            timeout = timeout,
            id = id,
            clock = clock,
            generateRequest = { it },
            installFeatures = { featureInstallers.forEach { it(this) } }
        )
    }
}

/**
 * Builder for Claude CLI agent with custom input type and structured output.
 */
public class ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output> internal constructor(
    config: AIAgentConfig,
    transport: CliTransport?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String?,
    permissionMode: ClaudePermissionMode?,
    additionalFlags: List<String>,
    internal val generateRequest: CliConfig.GenerateRequest<Input>,
    internal val structure: Structure<Output, LLMParams.Schema.JSON>,
) : ClaudeAgentBuilderBase<Input, CliAgentStructuredResponse<Output>, ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output>>(
    config, transport, workspace, timeout, id, clock, featureInstallers, apiKey, permissionMode, additionalFlags
) {
    override fun self(): ClaudeAgentGenericInputStructuredOutputBuilder<Input, Output> = this

    public fun build(): CliAIAgent<Input, CliAgentStructuredResponse<Output>> {
        val finalTransport = requireNotNull(this.transport) { "Transport is required" }
        return CliAIAgent.claude(
            transport = finalTransport,
            apiKey = apiKey,
            structure = structure,
            systemPrompt = null,
            llModel = null,
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

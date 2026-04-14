package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLModel
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Default builder for Codex CLI agent.
 */
public class CodexAgentBuilder internal constructor(
    transport: CliTransport,
    systemPrompt: String?,
    llModel: LLModel?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String? = null,
    sandbox: CodexSandboxMode? = null,
    askForApproval: CodexApprovalPolicy? = null,
    additionalFlags: List<String> = emptyList(),
) : CodexAgentBuilderBase<String, CodexAgentBuilder>(
    transport, systemPrompt, llModel, workspace, timeout, id, clock, featureInstallers, apiKey, sandbox, askForApproval, additionalFlags
) {
    override fun self(): CodexAgentBuilder = this

    /**
     * Configures a custom request generator for the agent.
     */
    public fun <Input> generateRequest(
        generateRequest: CliConfig.GenerateRequest<Input>
    ): CodexAgentGenericInputBuilder<Input> = CodexAgentGenericInputBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers,
        apiKey = apiKey,
        sandbox = sandbox,
        askForApproval = askForApproval,
        additionalFlags = additionalFlags,
        generateRequest = generateRequest
    )

    public fun build(): CliAIAgent<String, CliAIAgentResponse> {
        val finalTransport = requireNotNull(this.transport) { "Transport is required" }
        return CliAIAgent.codex(
            transport = finalTransport,
            apiKey = apiKey,
            systemPrompt = null,
            llModel = null,
            sandbox = sandbox,
            askForApproval = askForApproval,
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
 * Generic builder for Codex CLI agent with custom input type.
 */
public class CodexAgentGenericInputBuilder<Input> internal constructor(
    transport: CliTransport,
    systemPrompt: String?,
    llModel: LLModel?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    apiKey: String?,
    sandbox: CodexSandboxMode?,
    askForApproval: CodexApprovalPolicy?,
    additionalFlags: List<String>,
    internal val generateRequest: CliConfig.GenerateRequest<Input>,
) : CodexAgentBuilderBase<Input, CodexAgentGenericInputBuilder<Input>>(
    transport, systemPrompt, llModel, workspace, timeout, id, clock, featureInstallers, apiKey, sandbox, askForApproval, additionalFlags
) {
    override fun self(): CodexAgentGenericInputBuilder<Input> = this

    public fun build(): CliAIAgent<Input, CliAIAgentResponse> {
        val finalTransport = requireNotNull(this.transport) { "Transport is required" }
        return CliAIAgent.codex(
            transport = finalTransport,
            apiKey = apiKey,
            systemPrompt = null,
            llModel = null,
            sandbox = sandbox,
            askForApproval = askForApproval,
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

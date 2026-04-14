package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLModel
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Base class for Codex agent builders.
 */
public abstract class CodexAgentBuilderBase<Input, Self : CodexAgentBuilderBase<Input, Self>> internal constructor(
    transport: CliTransport,
    systemPrompt: String?,
    llModel: LLModel?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    protected var apiKey: String? = null,
    protected var sandbox: CodexSandboxMode? = null,
    protected var askForApproval: CodexApprovalPolicy? = null,
    protected var additionalFlags: List<String> = emptyList(),
) : CliAIAgentBuilderBase<Self>(
    transport,
    systemPrompt,
    llModel,
    workspace,
    timeout,
    id,
    clock,
    featureInstallers
) {
    /**
     * Sets the API key.
     */
    public fun apiKey(apiKey: String?): Self = self().apply {
        this.apiKey = apiKey
    }

    /**
     * Sets the sandbox mode.
     */
    public fun sandbox(sandbox: CodexSandboxMode): Self = self().apply {
        this.sandbox = sandbox
    }

    /**
     * Sets the approval policy.
     */
    public fun askForApproval(policy: CodexApprovalPolicy): Self = self().apply {
        this.askForApproval = policy
    }

    /**
     * Sets additional flags.
     */
    public fun additionalFlags(flags: List<String>): Self = self().apply {
        this.additionalFlags = flags
    }
}

package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliTransport
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Base class for Claude agent builders.
 */
public abstract class ClaudeAgentBuilderBase<Input, Output, Self : ClaudeAgentBuilderBase<Input, Output, Self>> internal constructor(
    config: AIAgentConfig,
    transport: CliTransport?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>,
    protected var apiKey: String? = null,
    protected var permissionMode: ClaudePermissionMode? = null,
    protected var additionalFlags: List<String> = emptyList(),
) : CliAIAgentBuilderBase<Self>(
    config,
    transport,
    workspace,
    timeout,
    id,
    clock,
    featureInstallers
) {
    public fun apiKey(apiKey: String?): Self = self().apply {
        this.apiKey = apiKey
    }

    public fun permissionMode(mode: ClaudePermissionMode): Self = self().apply {
        this.permissionMode = mode
    }

    public fun additionalFlags(flags: List<String>): Self = self().apply {
        this.additionalFlags = flags
    }
}

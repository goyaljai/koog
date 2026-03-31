package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.cli.CliAIAgentResponse
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliTransport
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Default builder for Claude CLI agent.
 */
public actual class ClaudeAgentBuilder internal actual constructor(
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
) : ClaudeAgentBuilderCommon<ClaudeAgentBuilder>(
    config, transport, workspace, timeout, id, clock, featureInstallers, apiKey, permissionMode, additionalFlags
) {
    public actual override fun self(): ClaudeAgentBuilder = this
}

/**
 * Builder for Claude CLI agent with custom input type.
 */
public actual class ClaudeAgentGenericInputBuilder<Input> internal actual constructor(
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
) : ClaudeAgentGenericInputBuilderCommon<Input, ClaudeAgentGenericInputBuilder<Input>>(
    config, transport, workspace, timeout, id, clock, featureInstallers, apiKey, permissionMode, additionalFlags, generateRequest
) {
    public actual override fun self(): ClaudeAgentGenericInputBuilder<Input> = this
}

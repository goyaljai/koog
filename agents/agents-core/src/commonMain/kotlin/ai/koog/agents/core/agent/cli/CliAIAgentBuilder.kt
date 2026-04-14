package ai.koog.agents.core.agent.cli

import ai.koog.cli.transport.CliTransport

/**
 * Builder for CLI AI agents.
 */
public class CliAgentBuilder internal constructor(transport: CliTransport) : CliAIAgentBuilderBase<CliAgentBuilder>(
    transport
) {
    override fun self(): CliAgentBuilder = this

    /**
     * Configures the agent to use Claude CLI.
     */
    public fun claude(): ClaudeAgentBuilder = ClaudeAgentBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers
    )

    /**
     * Configures the agent to use Codex CLI.
     */
    public fun codex(): CodexAgentBuilder = CodexAgentBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers
    )

    /**
     * Configures the agent with a custom CLI configuration.
     */
    public fun <Input, Output> custom(): CustomCliAgentBuilder<Input, Output> = CustomCliAgentBuilder(
        transport = transport,
        systemPrompt = systemPrompt,
        llModel = llModel,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers,
    )
}

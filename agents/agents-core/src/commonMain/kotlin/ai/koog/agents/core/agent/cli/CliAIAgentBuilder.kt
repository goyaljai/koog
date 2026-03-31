package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent.Companion.DummyModel
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.Prompt

/**
 * Builder for CLI AI agents.
 */
public class CliAgentBuilder internal constructor() : CliAIAgentBuilderBase<CliAgentBuilder>(
    AIAgentConfig(
        prompt = Prompt.Empty,
        model = DummyModel,
        maxAgentIterations = 10,
    )
) {
    override fun self(): CliAgentBuilder = this

    /**
     * Configures the agent to use Claude CLI.
     */
    public fun claude(): ClaudeAgentBuilder = ClaudeAgentBuilder(
        config = config,
        transport = transport,
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
        config = config,
        transport = transport,
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
        config = config,
        transport = transport,
        workspace = workspace,
        timeout = timeout,
        id = id,
        clock = clock,
        featureInstallers = featureInstallers,
    )
}

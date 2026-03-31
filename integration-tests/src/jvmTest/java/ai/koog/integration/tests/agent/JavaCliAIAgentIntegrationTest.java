package ai.koog.integration.tests.agent;

import ai.koog.agents.core.agent.CliAIAgent;
import ai.koog.agents.core.agent.cli.CliAIAgentResponse;
import ai.koog.agents.core.agent.cli.CliAgentStructuredResponse;
import ai.koog.cli.transport.CliTransport;
import ai.koog.integration.tests.base.KoogJavaTestBase;
import ai.koog.integration.tests.utils.StructuredResults;
import ai.koog.integration.tests.utils.TestCredentials;
import ai.koog.integration.tests.utils.annotations.Retry;
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels;
import ai.koog.prompt.executor.clients.openai.OpenAIModels;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JavaCliAIAgentIntegrationTest extends KoogJavaTestBase {

    private static class TestInput {
        public String request;

        public TestInput(String request) {
            this.request = request;
        }
    }

    private static String generateRequest(TestInput input) {
        return input.request;
    }

    private void testAgent(CliAIAgent<String, CliAIAgentResponse> agent) {
        var response = agent.run("echo 'hi'");
        assertResponse(response);
    }

    private void assertResponse(CliAIAgentResponse response) {
        assertResponse(response, "hi");
    }

    private void assertResponse(CliAIAgentResponse response, String expectedContent) {
        assertNotNull(response);
        assertFalse(response.isError(), "Run should be successful");
        var content = response.getContent();
        assertTrue(content.toLowerCase().contains(expectedContent.toLowerCase()),
            "Response should contain '" + expectedContent + "'");

        var usage = response.getUsage();
        assertNotNull(usage.getInputTokens(), "Usage should contain input tokens");
        assertNotNull(usage.getOutputTokens(), "Usage should contain output tokens");
    }

    private <T> void assertStructuredResponse(CliAgentStructuredResponse<T> response) {
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertNotNull(response.getResponse());
        assertFalse(response.getResponse().isError(), "Run should be successful");
    }

    @Test
    @Retry
    public void integration_testCodex() {
        var apiKey = TestCredentials.INSTANCE.readTestOpenAIKeyFromEnv();
        var agent = CliAIAgent.builder()
            .llModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .codex()
            .transport(CliTransport.getDefault())
            .apiKey(apiKey)
            .build();

        testAgent(agent);
    }

    @Test
    @Retry
    public void integration_testClaude() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = CliAIAgent.builder()
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .transport(CliTransport.getDefault())
            .apiKey(apiKey)
            .build();

        testAgent(agent);
    }

    @Test
    @Retry
    public void integration_testClaudeStructuredOutput() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = CliAIAgent.builder()
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .transport(CliTransport.getDefault())
            .apiKey(apiKey)
            .timeoutMin(1L)
            .structure(StructuredResults.CalculationResult.class)
            .build();

        var response = agent.run("what's 1 + 1?");
        assertStructuredResponse(response);
        assertEquals(2, response.getResult().getResult());
    }

    @Test
    @Retry
    public void integration_testClaudeCustomInput() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = CliAIAgent.builder()
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .transport(CliTransport.getDefault())
            .apiKey(apiKey)
            .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
            .build();

        var response = agent.run(new TestInput("echo 'hi'"));
        assertResponse(response);
    }

    @Test
    @Retry
    public void integration_testClaudeCustomInputStructuredOutput() {
        var apiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();
        var agent = CliAIAgent.builder()
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .transport(CliTransport.getDefault())
            .apiKey(apiKey)
            .timeoutMin(1L)
            .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
            .structure(StructuredResults.CalculationResult.class)
            .build();

        var response = agent.run(new TestInput("what's 1 + 1?"));
        assertStructuredResponse(response);
        assertEquals(2, response.getResult().getResult());
    }

    @Test
    @Retry
    public void integration_testCodexCustomInput() {
        var apiKey = TestCredentials.INSTANCE.readTestOpenAIKeyFromEnv();
        var agent = CliAIAgent.builder()
            .llModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .codex()
            .transport(CliTransport.getDefault())
            .apiKey((String) apiKey)
            .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
            .build();

        var response = agent.run(new TestInput("echo 'hi'"));
        assertResponse(response);
    }
}

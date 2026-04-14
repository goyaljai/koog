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
        assertResponse(response, "hi");
    }

    private void assertResponse(CliAIAgentResponse response, String expectedContent) {
        assertNotNull(response);
        assertFalse(response.isError(), "Run should be successful");
        var content = response.getContent();
        assertTrue(content.toLowerCase().contains(expectedContent.toLowerCase()),
            "Response should contain '" + expectedContent + "'");

        var metaInfo = response.getMetaInfo();
        assertNotNull(metaInfo.getInputTokensCount(), "Usage should contain input tokens");
        assertNotNull(metaInfo.getOutputTokensCount(), "Usage should contain output tokens");
    }

    private final String openaiApiKey = TestCredentials.INSTANCE.readTestOpenAIKeyFromEnv();
    private final String anthropicApiKey = TestCredentials.INSTANCE.readTestAnthropicKeyFromEnv();


    private <T> void assertStructuredResponseIsSuccessful(CliAgentStructuredResponse<T> response) {
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertNotNull(response.getResponse());
        assertFalse(response.getResponse().isError(), "Run should be successful");
    }

    @Test
    @Retry
    public void integration_testCodex() {
        var agent = CliAIAgent.builder(CliTransport.getDefault())
            .llModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .codex()
            .apiKey(openaiApiKey)
            .build();

        testAgent(agent);
    }

    @Test
    @Retry
    public void integration_testClaude() {
        var agent = CliAIAgent.builder(CliTransport.getDefault())
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .apiKey(anthropicApiKey)
            .build();

        testAgent(agent);
    }

    @Test
    @Retry
    public void integration_testClaudeStructuredOutput() {
        var agent = CliAIAgent.builder(CliTransport.getDefault())
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .apiKey(anthropicApiKey)
            .timeoutMin(1L)
            .structure(StructuredResults.CalculationResult.class)
            .build();

        var response = agent.run("what's 1 + 1?");
        assertStructuredResponseIsSuccessful(response);
        assertEquals(2, response.getResult().getResult());
    }

    @Test
    @Retry
    public void integration_testClaudeCustomInput() {
        var agent = CliAIAgent.builder(CliTransport.getDefault())
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .apiKey(anthropicApiKey)
            .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
            .build();

        var response = agent.run(new TestInput("echo 'hi'"));
        assertResponse(response, "hi");
    }

    @Test
    @Retry
    public void integration_testClaudeCustomInputStructuredOutput() {
        var agent = CliAIAgent.builder(CliTransport.getDefault())
            .llModel(AnthropicModels.Sonnet_4_5)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .claude()
            .apiKey(anthropicApiKey)
            .timeoutMin(1L)
            .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
            .structure(StructuredResults.CalculationResult.class)
            .build();

        var response = agent.run(new TestInput("what's 1 + 1?"));
        assertStructuredResponseIsSuccessful(response);
        assertEquals(2, response.getResult().getResult());
    }

    @Test
    @Retry
    public void integration_testCodexCustomInput() {
        var agent = CliAIAgent.builder(CliTransport.getDefault())
            .llModel(OpenAIModels.Chat.GPT4o)
            .systemPrompt("please follow the instructions of the user. do not call any tools")
            .codex()
            .apiKey(openaiApiKey)
            .generateRequest(JavaCliAIAgentIntegrationTest::generateRequest)
            .build();

        var response = agent.run(new TestInput("echo 'hi'"));
        assertResponse(response, "hi");
    }
}

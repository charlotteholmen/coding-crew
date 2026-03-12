package net.osgiliath.codeprompt.cucumber;

import com.agentclientprotocol.model.ContentBlock;
import io.cucumber.spring.CucumberContextConfiguration;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.graph.PromptGraph;
import net.osgiliath.codeprompt.CodePromptFrameworkApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cucumber Spring configuration that sets up the Spring Boot context for BDD tests.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = {CodePromptFrameworkApplication.class})
public class CucumberSpringConfiguration {

    /**
     * Mock CommandLineRunners to prevent them from starting and blocking stdin.
     */
    @MockitoBean
    private CommandLineRunner commandLineRunner;


    /**
     * Provide a mock AcpAgentSupportBridge bean for testing.
     */
    @Bean
    public AcpAgentSupportBridge acpAgentSupportBridge() {
        return new AcpAgentSupportBridge() {
            @Override
            public AgentInfoBridge getAgentInfo() {
                return new AgentInfoBridge("Test Agent", "1.0");
            }

            @Override
            public AcpSessionBridge createSession(String sessionId, String cwd, Map<String, String> mcpServers) {
                return new AcpSessionBridge() {
                    private final AtomicBoolean cancelled = new AtomicBoolean(false);

                    @Override
                    public String getSessionId() {
                        return sessionId;
                    }

                    @Override
                    public AtomicBoolean cancelledFlag() {
                        return cancelled;
                    }

                    @Override
                    public CompletableFuture<String> processPrompt(String promptText, java.util.List<ContentBlock.ResourceLink> resourceLinks) {
                        String response = buildMockResponse(promptText, resourceLinks);
                        return CompletableFuture.completedFuture(response);
                    }

                    @Override
                    public void streamPrompt(String promptText, java.util.List<ContentBlock.ResourceLink> resourceLinks, TokenConsumer consumer) {
                        String response = buildMockResponse(promptText, resourceLinks);
                        for (String token : response.split(" ")) {
                            consumer.onNext(token + " ");
                        }
                        consumer.onComplete();
                    }

                    private String buildMockResponse(String promptText, java.util.List<ContentBlock.ResourceLink> resourceLinks) {
                        if (resourceLinks != null && !resourceLinks.isEmpty()) {
                            // Include "attachment considered" when processing ResourceLinks
                            return "Mock response for: " + promptText +
                                   ". Attachment considered. Resource analysis complete. Content reviewed.";
                        }
                        return "Mock response: " + promptText;
                    }
                };
            }
        };
    }

    /**
     * Provide a LangChain4jAdapter bean for testing.
     */
    @Bean
    public LangGraph4jAdapter langChain4jAdapter(PromptGraph promptGraph) {
        return new LangGraph4jAdapter(promptGraph);
    }

/*
    static OllamaContainer ollamaContainer;
    @BeforeAll
    public static void before_all() throws IOException, InterruptedException {
        System.setProperty("api.version", "1.44");
        ollamaContainer = new OllamaContainer(
            DockerImageName.parse("ollama/ollama")
        ).withReuse(true);
        ollamaContainer.start();
        ollamaContainer.execInContainer("ollama", "pull", "gemma3:1b");
    }

    @AfterAll
    public static void after_all() {
        if (ollamaContainer != null) {
            ollamaContainer.stop();
        }
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("langchain4j.ollama.chat-model.base-url", () -> "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434));
        registry.add("langchain4j.ollama.streaming-chat-model.base-url", () -> "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434));
    }
    */
 }

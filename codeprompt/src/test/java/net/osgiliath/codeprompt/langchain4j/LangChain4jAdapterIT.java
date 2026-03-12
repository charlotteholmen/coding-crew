package net.osgiliath.codeprompt.langchain4j;

import com.agentclientprotocol.model.ContentBlock;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.LangGraph4jAdapter;
import net.osgiliath.codeprompt.CodePromptFrameworkApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = CodePromptFrameworkApplication.class, properties = {
        "spring.main.web-application-type=none"
})
public class LangChain4jAdapterIT {
    @Autowired
    private LangGraph4jAdapter adapter;

    // Mock CommandLineRunners to prevent them from starting and blocking stdin
    @MockitoBean
    private CommandLineRunner commandLineRunner;
    
    @Test
    void contextLoads() {
        assertThat(adapter).isNotNull();
    }

    @Test
    void testProcessPromptWithAssistant() {
        String prompt = "Hello AI, please respond with exactly the word 'ACK'";

        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();
        adapter.streamPrompt(prompt, Collections.EMPTY_LIST, new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }
        });

        await().atMost(10, SECONDS).untilTrue(completed);
        assertThat(error.get()).isNull();
        String response = fullResponse.toString();
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        // We can't guarantee 'ACK' perfectly, but we can check it contains it
        assertThat(response).containsIgnoringCase("ACK");
    }

    @Test
    void testStreamPromptWithAssistant() {
        String prompt = "Hello AI, please respond with exactly the word 'ACK', do not use any tool";
        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        adapter.streamPrompt(prompt, Collections.EMPTY_LIST, new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }
        });

        await().atMost(10, SECONDS).untilTrue(completed);
        assertThat(error.get()).isNull();
        assertThat(fullResponse.toString()).isNotNull();
        assertThat(fullResponse.toString()).isNotEmpty();
        assertThat(fullResponse.toString()).containsIgnoringCase("ACK");
    }

    @Test
    void testStreamPromptWithResourceLinkToThreadJava() {
        // Create ResourceLink to Thread.java in test dataset
        Path threadJavaPath = Paths.get("src/test/resources/dataset/Thread.java").toAbsolutePath();
        File threadJavaFile = threadJavaPath.toFile();

        assertThat(threadJavaFile).exists();

        ContentBlock.ResourceLink threadJavaLink = new ContentBlock.ResourceLink(
                "Thread.java",
                "file://" + threadJavaPath,
                "Mock Thread.java class from test dataset",
                "text/java",
                threadJavaFile.length(),
                "Thread.java",
                null,  // annotations
                null   // _meta
        );

        String prompt = "Please analyze the attached Thread.java file and extract the secret code from the EASTER EGG comment. " +
                "Answer with the EGG: the word only which starts with 'CUCUMBER_'.";

        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        adapter.streamPrompt(prompt, List.of(threadJavaLink), new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }
        });

        await().atMost(30, SECONDS).untilTrue(completed);
        assertThat(error.get()).isNull();
        String response = fullResponse.toString();
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        // Verify the AI read the file and extracted the secret code from the EASTER EGG comment
        assertThat(response).containsIgnoringCase("CUCUMBER_BDD_ROCKS_2026");
    }

}

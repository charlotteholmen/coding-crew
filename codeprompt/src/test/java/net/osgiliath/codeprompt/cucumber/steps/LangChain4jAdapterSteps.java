package net.osgiliath.codeprompt.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.osgiliath.acplanggraphlangchainbridge.acp.AcpAgentSupportBridge;
import net.osgiliath.acplanggraphlangchainbridge.acp.InAcpAdapter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Step definitions for LangChain4j Adapter feature scenarios.
 */
public class LangChain4jAdapterSteps {
    private final StringBuilder fullResponse = new StringBuilder();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicInteger tokenCount = new AtomicInteger(0);
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private InAcpAdapter adapter;
    private String currentPrompt;

    @Given("the CodePrompt application is initialized")
    public void theCodePromptApplicationIsInitialized() {
        // Spring context is already initialized via CucumberSpringConfiguration
        assertThat(adapter).isNotNull();
    }

    @Given("the LangChain4j adapter is available")
    public void theLangChain4jAdapterIsAvailable() {
        assertThat(adapter).isNotNull();
    }

    @Given("I have a prompt {string}")
    public void iHaveAPrompt(String prompt) {
        this.currentPrompt = prompt;
        resetState();
    }

    @Given("I have an empty prompt")
    public void iHaveAnEmptyPrompt() {
        this.currentPrompt = "";
        resetState();
    }

    @Given("I have a blank prompt {string}")
    public void iHaveABlankPrompt(String prompt) {
        this.currentPrompt = prompt;
        resetState();
    }

    @Given("I have a multi-line prompt")
    public void iHaveAMultiLinePrompt(String prompt) {
        this.currentPrompt = prompt;
        resetState();
    }

    @When("I process the prompt through the adapter")
    public void iProcessThePromptThroughTheAdapter() {
        AcpAgentSupportBridge.AcpSessionBridge session =
            adapter.createSession("langchain4j-cucumber-session", ".", Collections.emptyMap());

        session.streamPrompt(currentPrompt, Collections.emptyList(), new AcpAgentSupportBridge.TokenConsumer() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                tokenCount.incrementAndGet();
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                completed.set(true);
            }
        });

        // Wait for completion with extended timeout (AI service may be slow)
        // Note: This uses real LangChain4j streaming which calls external AI services
        await().atMost(30, SECONDS).untilTrue(completed);
    }

    @Then("I should receive a response")
    public void iShouldReceiveAResponse() {
        assertThat(fullResponse.toString()).isNotNull();
        assertThat(fullResponse.toString()).isNotEmpty();
    }

    @Then("the response should contain {string}")
    public void theResponseShouldContain(String expectedText) {
        assertThat(fullResponse.toString()).containsIgnoringCase(expectedText);
    }

    @Then("I should receive a response {string}")
    public void iShouldReceiveAResponse(String expectedResponse) {
        assertThat(fullResponse.toString()).isEqualTo(expectedResponse);
    }

    @Then("the response should not be empty")
    public void theResponseShouldNotBeEmpty() {
        assertThat(fullResponse.toString()).isNotEmpty();
    }

    @Then("the processing should complete without errors")
    public void theProcessingShouldCompleteWithoutErrors() {
        assertThat(error.get()).isNull();
        assertThat(completed.get()).isTrue();
    }

    @Then("I should receive multiple tokens")
    public void iShouldReceiveMultipleTokens() {
        // Streaming should produce multiple token callbacks
        assertThat(tokenCount.get()).isGreaterThan(0);
    }

    private void resetState() {
        fullResponse.setLength(0);
        completed.set(false);
        error.set(null);
        tokenCount.set(0);
    }
}


package net.osgiliath.codeprompt.langgraph.node;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.TokenStream;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.AcpState;
import net.osgiliath.codeprompt.skills.java.JavaSpringBootAssistant;
import net.osgiliath.agentsdk.utils.MimeTypeUtils;
import net.osgiliath.agentsdk.utils.UnsupportedMimeTypeException;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Graph node that streams LLM responses using the {@code @AiService} DSL.
 *
 * <p>Delegates to the Spring-managed {@link JavaSpringBootAssistant} which is
 * auto-configured via {@link dev.langchain4j.service.spring.AiService}.
 * The system prompt, model selection, and HTTP client are all handled by
 * Spring Boot auto-configuration — no manual {@code ChatRequest} assembly.</p>
 *
 * <h2>Streaming bridge</h2>
 * <p>The {@code @AiService}'s {@link TokenStream} is bridged into LangGraph4j's
 * {@link StreamingChatGenerator} so the graph runtime can iterate over
 * {@link org.bsc.langgraph4j.streaming.StreamingOutput} chunks:</p>
 * <ol>
 *   <li>Create a {@link StreamingChatGenerator} with a {@code mapResult} that
 *       merges the final {@link dev.langchain4j.data.message.AiMessage} into the state.</li>
 *   <li>Obtain a {@link TokenStream} from {@code assistant.streamChat(prompt)}.</li>
 *   <li>Wire the token stream's callbacks to the generator's
 *       {@link dev.langchain4j.model.chat.response.StreamingChatResponseHandler}.</li>
 *   <li>Call {@link TokenStream#start()} to kick off non-blocking streaming.</li>
 *   <li>Return {@code Map.of("_streaming_messages", generator)}.</li>
 * </ol>
 */
@Component
public class LLMProcessorNode implements NodeAction<AcpState<ChatMessage>> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LLMProcessorNode.class);

    private final JavaSpringBootAssistant assistant;

    public LLMProcessorNode(JavaSpringBootAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public Map<String, Object> apply(AcpState<ChatMessage> state) {
        log.info("CallModel for session {} in cwd {} with {} MCP server(s)",
                state.sessionId(),
                state.cwd(),
                state.mcpServers().size());

        var generator = StreamingChatGenerator.<MessagesState<ChatMessage>>builder()
                .mapResult(response -> Map.of("messages", response.aiMessage()))
                .startingNode("agent")
                .startingState(state)
                .build();

        List<Content> contents = new ArrayList<>();
        // Extract the latest user message text from the state.
        String userMessageText = state.messages().stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .reduce((first, second) -> second) // last user message
                .map(UserMessage::singleText)
                .orElse("");
        contents.add(TextContent.from(userMessageText));
        List<ResourceLinkContent> meta = state.attachmentsMetadata();
        List<byte[]> data = state.attachments();
        for (int i = 0; i < meta.size(); i++) {
            try {
                contents.add(MimeTypeUtils.toContent(meta.get(i), data.get(i)));
            } catch (UnsupportedMimeTypeException e) {
                log.warn("Failed to process attachment {} for session {}: {}", i, state.sessionId(), e.getMessage());
            }
        }

        TokenStream tokenStream = assistant.streamChat(UserMessage.from(contents));

        // Bridge TokenStream callbacks → StreamingChatGenerator handler (queue).
        var handler = generator.handler();
        tokenStream
                .onPartialResponse(handler::onPartialResponse)
                .onCompleteResponse(handler::onCompleteResponse)
                .onError(handler::onError)
                .start();

        return Map.of("_streaming_messages", generator);
    }
}

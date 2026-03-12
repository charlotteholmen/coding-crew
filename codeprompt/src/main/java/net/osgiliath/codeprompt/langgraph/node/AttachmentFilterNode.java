package net.osgiliath.codeprompt.langgraph.node;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.node.attachment.AttachmentMetadataDTO;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.node.attachment.AttachmentsMetadata;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.AcpState;
import net.osgiliath.codeprompt.skills.attachment.AttachmentFiltererFromMetadata;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AttachmentFilterNode implements NodeAction<AcpState<ChatMessage>> {
    private static final Logger log = LoggerFactory.getLogger(AttachmentFilterNode.class);
    private final AttachmentFiltererFromMetadata selector;

    public AttachmentFilterNode(AttachmentFiltererFromMetadata selector) {
        this.selector = selector;
    }

    @Override
    public Map<String, Object> apply(AcpState<ChatMessage> state) {
        log.debug("Filtering metadata for session {} question: {} with attachments: {}",
                state.sessionId(),
                state.messages(),
                state.attachmentsMetadata());

        String combinedMessages = state.messages().stream()
                .map(this::extractText)
                .collect(Collectors.joining("\n"));

        AttachmentsMetadata response = selector.chat(
                combinedMessages,
                state.attachmentsMetadata()
        );

        List<ResourceLinkContent> filtered = response.attachments().stream()
                .map(dto -> findOriginal(dto, state.attachmentsMetadata()))
                .filter(Objects::nonNull)
                .toList();

        return Map.of(
                AcpState.ATTACHMENTS_META, filtered
        );
    }

    private ResourceLinkContent findOriginal(AttachmentMetadataDTO dto, List<ResourceLinkContent> originals) {
        return originals.stream()
                .filter(original -> Objects.equals(original.name(), dto.name()))
                .findFirst()
                .orElse(null);
    }

    private String extractText(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        return message.toString();
    }
}

package net.osgiliath.codeprompt.skills.attachment;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.node.attachment.AttachmentsMetadata;

import java.util.List;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;
import static net.osgiliath.agentsdk.configuration.LangChain4jConfig.TOOL_PROVIDER_NONE;

@AiService(wiringMode = EXPLICIT, toolProvider = TOOL_PROVIDER_NONE, chatModel = "primaryChatModel")
public interface AttachmentFiltererFromMetadata {
     /**
     * Chat with the assistant using a simple string message.
     *
     * @param userMessage the user's message
     * @return the assistant's response
     */
    @SystemMessage("You are a helpful assistant specialized in selecting attachment of interest for answering a question.")
    @UserMessage("{{userMessage}}\n\nHere are the available attachments:\n{{attachments}}\n\nRespond with a with the list of useful elements")
    AttachmentsMetadata chat(@V("userMessage") String userMessage, @V("attachments") List<ResourceLinkContent> attachmentMetadata);
}

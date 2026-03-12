package net.osgiliath.codeprompt.skills.java;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;
import static net.osgiliath.agentsdk.configuration.LangChain4jConfig.TOOL_PROVIDER_NONE;

/**
 * Simple AI assistant interface powered by LangChain4j.
 * Spring Boot will automatically create an implementation of this interface.
 */
@AiService(wiringMode = EXPLICIT, toolProvider = TOOL_PROVIDER_NONE, streamingChatModel = "primaryStreamingChatModel")
public interface JavaSpringBootAssistant {

    /**
     * Chat with the assistant using a simple string message with streaming.
     *
     * @param userMessage the user's message
     * @return the assistant's response stream
     */
    @SystemMessage("You are a helpful coding assistant specialized in Java, Spring Boot, and enterprise applications.")
    TokenStream streamChat(UserMessage userMessage);
}



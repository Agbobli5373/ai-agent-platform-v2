package com.platform.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class MistralAIConfig {

    @ConfigProperty(name = "quarkus.langchain4j.mistralai.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.langchain4j.mistralai.chat-model.model-name", defaultValue = "mistral-large-latest")
    String modelName;

    @ConfigProperty(name = "quarkus.langchain4j.mistralai.chat-model.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "quarkus.langchain4j.mistralai.chat-model.max-tokens", defaultValue = "2000")
    Integer maxTokens;

    @Produces
    @Named("defaultChatModel")
    @ApplicationScoped
    public ChatLanguageModel defaultChatModel() {
        return MistralAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Produces
    @Named("streamingChatModel")
    @ApplicationScoped
    public ChatLanguageModel streamingChatModel() {
        return MistralAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}

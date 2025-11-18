package com.platform.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MistralAIConfig.
 */
@QuarkusTest
class MistralAIConfigTest {

    @Inject
    @Named("defaultChatModel")
    ChatLanguageModel defaultChatModel;

    @Inject
    @Named("streamingChatModel")
    ChatLanguageModel streamingChatModel;

    @Test
    void testDefaultChatModelIsInjected() {
        assertNotNull(defaultChatModel);
    }

    @Test
    void testStreamingChatModelIsInjected() {
        assertNotNull(streamingChatModel);
    }

    @Test
    void testBothModelsAreDistinct() {
        // Both models should be separate instances
        assertNotSame(defaultChatModel, streamingChatModel);
    }
}

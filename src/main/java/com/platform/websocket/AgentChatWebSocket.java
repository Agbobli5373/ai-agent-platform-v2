package com.platform.websocket;

import com.platform.service.AgentRuntimeService;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time agent chat interactions.
 * Supports streaming responses from AI agents.
 */
@WebSocket(path = "/ws/agent/{agentId}/chat")
public class AgentChatWebSocket {

    @Inject
    AgentRuntimeService runtimeService;

    // Store conversation IDs per connection
    private static final ConcurrentHashMap<String, UUID> conversationMap = new ConcurrentHashMap<>();

    /**
     * Handle WebSocket connection open.
     */
    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection) {
        String connectionId = connection.id();
        String agentId = connection.pathParam("agentId");

        Log.infof("WebSocket connection opened: %s for agent: %s", connectionId, agentId);

        return Uni.createFrom().voidItem();
    }

    /**
     * Handle incoming text messages from client.
     * Processes the message and streams the AI response back.
     */
    @OnTextMessage
    public Multi<String> onMessage(String message, WebSocketConnection connection) {
        String connectionId = connection.id();
        String agentIdParam = connection.pathParam("agentId");

        try {
            UUID agentId = UUID.fromString(agentIdParam);

            // Parse message - expect JSON format: {"message": "...", "userId": "...",
            // "conversationId": "..."}
            ChatMessage chatMessage = parseMessage(message);

            // Get or create conversation ID for this connection
            UUID conversationId = conversationMap.get(connectionId);
            if (chatMessage.conversationId != null) {
                conversationId = UUID.fromString(chatMessage.conversationId);
                conversationMap.put(connectionId, conversationId);
            }

            UUID userId = chatMessage.userId != null ? UUID.fromString(chatMessage.userId) : null;

            Log.infof("Processing message for agent %s, conversation %s", agentId, conversationId);

            // Stream response from agent runtime service
            return runtimeService.streamResponse(agentId, chatMessage.message, conversationId, userId)
                    .onItem().invoke(token -> {
                        // Log progress (optional)
                        if (Log.isDebugEnabled()) {
                            Log.debugf("Streaming token: %s", token);
                        }
                    })
                    .onFailure().invoke(failure -> {
                        Log.errorf(failure, "Error streaming response for agent %s", agentId);
                    });

        } catch (IllegalArgumentException e) {
            Log.errorf(e, "Invalid agent ID or message format: %s", agentIdParam);
            return Multi.createFrom().item("Error: Invalid request format");
        } catch (Exception e) {
            Log.errorf(e, "Error processing message for connection %s", connectionId);
            return Multi.createFrom().item("Error: " + e.getMessage());
        }
    }

    /**
     * Handle WebSocket connection close.
     */
    @OnClose
    public Uni<Void> onClose(WebSocketConnection connection) {
        String connectionId = connection.id();
        UUID conversationId = conversationMap.remove(connectionId);

        Log.infof("WebSocket connection closed: %s, conversation: %s", connectionId, conversationId);

        return Uni.createFrom().voidItem();
    }

    /**
     * Handle WebSocket errors.
     */
    @OnError
    public Uni<Void> onError(WebSocketConnection connection, Throwable error) {
        String connectionId = connection.id();
        Log.errorf(error, "WebSocket error for connection: %s", connectionId);

        return Uni.createFrom().voidItem();
    }

    /**
     * Parse incoming message JSON.
     * Simple parsing - in production, use Jackson or similar.
     */
    private ChatMessage parseMessage(String json) {
        ChatMessage msg = new ChatMessage();

        // Simple JSON parsing (for production, use proper JSON library)
        if (json.contains("\"message\"")) {
            int start = json.indexOf("\"message\"") + 11;
            int end = json.indexOf("\"", start);
            if (end > start) {
                msg.message = json.substring(start, end);
            }
        }

        if (json.contains("\"userId\"")) {
            int start = json.indexOf("\"userId\"") + 11;
            int end = json.indexOf("\"", start);
            if (end > start) {
                msg.userId = json.substring(start, end);
            }
        }

        if (json.contains("\"conversationId\"")) {
            int start = json.indexOf("\"conversationId\"") + 18;
            int end = json.indexOf("\"", start);
            if (end > start) {
                msg.conversationId = json.substring(start, end);
            }
        }

        return msg;
    }

    /**
     * Simple DTO for chat messages.
     */
    private static class ChatMessage {
        String message;
        String userId;
        String conversationId;
    }
}
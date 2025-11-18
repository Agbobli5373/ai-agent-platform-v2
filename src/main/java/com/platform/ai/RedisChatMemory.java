package com.platform.ai;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RedisChatMemory {
    private static final Logger LOG = Logger.getLogger(RedisChatMemory.class);
    private static final String MEMORY_KEY_PREFIX = "chat:memory:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    @Inject
    RedisDataSource redisDataSource;
    private ValueCommands<String, String> valueCommands;

    public void storeMessage(Long conversationId, String role, String content) {
        try {
            String key = buildKey(conversationId);
            String message = role + ": " + content;
            List<String> messages = getMessages(conversationId);
            messages.add(message);
            String conversationData = String.join("\n", messages);
            getValueCommands().setex(key, DEFAULT_TTL.getSeconds(), conversationData);
            LOG.debugf("Stored message in conversation %d: %s", conversationId, message);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to store message in conversation %d", conversationId);
        }
    }

    public List<String> getMessages(Long conversationId) {
        try {
            String key = buildKey(conversationId);
            String conversationData = getValueCommands().get(key);
            if (conversationData == null || conversationData.isEmpty()) {
                return new ArrayList<>();
            }
            return List.of(conversationData.split("\n"));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve messages from conversation %d", conversationId);
            return new ArrayList<>();
        }
    }

    public String getConversationHistory(Long conversationId) {
        List<String> messages = getMessages(conversationId);
        return String.join("\n", messages);
    }

    public void clearMemory(Long conversationId) {
        try {
            String key = buildKey(conversationId);
            getValueCommands().getdel(key);
            LOG.debugf("Cleared memory for conversation %d", conversationId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to clear memory for conversation %d", conversationId);
        }
    }

    public void updateTTL(Long conversationId, Duration ttl) {
        try {
            String key = buildKey(conversationId);
            String existingData = getValueCommands().get(key);
            if (existingData != null) {
                getValueCommands().setex(key, ttl.getSeconds(), existingData);
                LOG.infof("Updated TTL for conversation %d", conversationId);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to update TTL for conversation %d", conversationId);
        }
    }

    public boolean hasMemory(Long conversationId) {
        try {
            String key = buildKey(conversationId);
            return getValueCommands().get(key) != null;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to check memory existence for conversation %d", conversationId);
            return false;
        }
    }

    private String buildKey(Long conversationId) {
        return MEMORY_KEY_PREFIX + conversationId;
    }

    private ValueCommands<String, String> getValueCommands() {
        if (valueCommands == null) {
            valueCommands = redisDataSource.value(String.class, String.class);
        }
        return valueCommands;
    }
}ntext.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RedisChatMemory {
    private static final Logger LOG = Logger.getLogger(RedisChatMemory.class);
    private static final String MEMORY_KEY_PREFIX = "chat:memory:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    @Inject
    RedisDataSource redisDataSource;
    private ValueCommands<String, String> valueCommands;

    public void storeMessage(Long conversationId, String role, String content) {
        try {
            String key = buildKey(conversationId);
            String message = role + ": " + content;
            List<String> messages = getMessages(conversationId);
            messages.add(message);
            String conversationData = String.join("\n", messages);
            getValueCommands().setex(key, DEFAULT_TTL.getSeconds(), conversationData);
            LOG.debugf("Stored message in conversation %d: %s", conversationId, message);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to store message in conversation %d", conversationId);
        }
    }

    public List<String> getMessages(Long conversationId) {
        try {
            String key = buildKey(conversationId);
            String conversationData = getValueCommands().get(key);
            if (conversationData == null || conversationData.isEmpty()) {
                return new ArrayList<>();
            }
            return List.of(conversationData.split("\n"));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve messages from conversation %d", conversationId);
            return new ArrayList<>();
        }
    }

    public String getConversationHistory(Long conversationId) {
        List<String> messages = getMessages(conversationId);
        return String.join("\n", messages);
    }

    public void clearMemory(Long conversationId) {
        try {
            String key = buildKey(conversationId);
            getValueCommands().getdel(key);
            LOG.debugf("Cleared memory for conversation %d", conversationId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to clear memory for conversation %d", conversationId);
        }
    }

    public void updateTTL(Long conversationId, Duration ttl) {
        try {
            String key = buildKey(conversationId);
            String existingData = getValueCommands().get(key);
            if (existingData != null) {
                getValueCommands().setex(key, ttl.getSeconds(), existingData);
                LOG.infof("Updated TTL for conversation %d", conversationId);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to update TTL for conversation %d", conversationId);
        }
    }

    public boolean hasMemory(Long conversationId) {
        try {
            String key = buildKey(conversationId);
            return getValueCommands().get(key) != null;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to check memory existence for conversation %d", conversationId);
            return false;
        }
    }

    private String buildKey(Long conversationId) {
        return MEMORY_KEY_PREFIX + conversationId;
    }

    private ValueCommands<String, String> getValueCommands() {
        if (valueCommands == null) {
            valueCommands = redisDataSource.value(String.class, String.class);
        }
        return valueCommands;
    }
}
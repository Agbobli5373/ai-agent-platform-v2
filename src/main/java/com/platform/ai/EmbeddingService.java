package com.platform.ai;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EmbeddingService {
    private static final Logger LOG = Logger.getLogger(EmbeddingService.class);

    @ConfigProperty(name = "quarkus.langchain4j.mistralai.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.langchain4j.mistralai.embedding-model.model-name", defaultValue = "mistral-embed")
    String modelName;

    private EmbeddingModel embeddingModel;

    @PostConstruct
    void init() {
        this.embeddingModel = MistralAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
        LOG.infof("Initialized Mistral AI embedding model: %s", modelName);
    }

    public float[] embed(String text) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            List<Float> vectorList = embedding.vectorAsList();
            float[] result = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                result[i] = vectorList.get(i);
            }
            return result;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate embedding for text: %s", text);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public List<float[]> embedAll(List<String> texts) {
        try {
            List<TextSegment> segments = texts.stream()
                    .map(TextSegment::from)
                    .collect(Collectors.toList());
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            return embeddings.stream()
                    .map(embedding -> {
                        List<Float> vectorList = embedding.vectorAsList();
                        float[] result = new float[vectorList.size()];
                        for (int i = 0; i < vectorList.size(); i++) {
                            result[i] = vectorList.get(i);
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate embeddings for %d texts", texts.size());
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }

    public int getDimension() {
        return 1024;
    }
}

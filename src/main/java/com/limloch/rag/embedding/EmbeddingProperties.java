package com.limloch.rag.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag.embedding")
public class EmbeddingProperties {

    private String provider = "mock";
    private int dimensions = 1536;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getDimensions() { return dimensions; }
    public void setDimensions(int dimensions) { this.dimensions = dimensions; }
}
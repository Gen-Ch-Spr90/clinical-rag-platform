package com.limloch.rag.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag.chunking")
public class ChunkingProperties {

    private int sizeTokens = 500;
    private int overlapTokens = 50;

    public int getSizeTokens() { return sizeTokens; }
    public void setSizeTokens(int sizeTokens) { this.sizeTokens = sizeTokens; }

    public int getOverlapTokens() { return overlapTokens; }
    public void setOverlapTokens(int overlapTokens) { this.overlapTokens = overlapTokens; }
}
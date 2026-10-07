package com.limloch.rag.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag.llm")
public class LlmProperties {

    private String provider = "mock";
    private int maxTokens = 1024;
    private double temperature = 0.2;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}
package com.zyntral.modules.ai.application;

import com.zyntral.modules.ai.config.AiProperties;
import com.zyntral.modules.ai.domain.AiProviderKind;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves an {@link AiProvider} by kind, falling back to the configured default. Spring
 * injects every {@link AiProvider} bean, so registering a new vendor is zero-config here.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderRegistry {

    private final Map<AiProviderKind, AiProvider> providers = new EnumMap<>(AiProviderKind.class);
    private final AiProviderKind defaultKind;

    public AiProviderRegistry(List<AiProvider> all, AiProperties props) {
        all.forEach(p -> providers.put(p.kind(), p));
        this.defaultKind = props.defaultProvider();
    }

    public AiProvider resolve(AiProviderKind kind) {
        AiProviderKind effective = kind != null ? kind : defaultKind;
        AiProvider provider = providers.get(effective);
        if (provider == null) {
            throw new IllegalStateException("No AI provider registered for " + effective);
        }
        return provider;
    }

    public AiProviderKind defaultKind() {
        return defaultKind;
    }
}

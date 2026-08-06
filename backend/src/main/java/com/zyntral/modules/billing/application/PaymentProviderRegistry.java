package com.zyntral.modules.billing.application;

import com.zyntral.modules.billing.config.PaymentProperties;
import com.zyntral.modules.billing.domain.PaymentProviderKind;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resolves a {@link PaymentProvider} by kind. Spring injects all adapter beans. */
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentProviderRegistry {

    private final Map<PaymentProviderKind, PaymentProvider> providers =
            new EnumMap<>(PaymentProviderKind.class);

    public PaymentProviderRegistry(List<PaymentProvider> all) {
        all.forEach(p -> providers.put(p.kind(), p));
    }

    public PaymentProvider resolve(PaymentProviderKind kind) {
        PaymentProvider provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalStateException("No payment provider configured for " + kind);
        }
        return provider;
    }
}

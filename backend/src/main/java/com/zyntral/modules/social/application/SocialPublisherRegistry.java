package com.zyntral.modules.social.application;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resolves the {@link SocialPublisher} for a platform. Spring injects all adapter beans. */
@Component
public class SocialPublisherRegistry {

    private final Map<SocialPlatform, SocialPublisher> publishers =
            new EnumMap<>(SocialPlatform.class);

    public SocialPublisherRegistry(List<SocialPublisher> all) {
        all.forEach(p -> publishers.put(p.platform(), p));
    }

    public SocialPublisher forPlatform(SocialPlatform platform) {
        SocialPublisher publisher = publishers.get(platform);
        if (publisher == null) {
            throw new IllegalStateException("No publisher registered for " + platform);
        }
        return publisher;
    }
}

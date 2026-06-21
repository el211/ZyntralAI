package com.zyntral.modules.social.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resolves the {@link SocialOAuthProvider} for a platform (only those wired for OAuth). */
@Configuration
@EnableConfigurationProperties(SocialOAuthProperties.class)
public class SocialOAuthRegistry {

    private final Map<SocialPlatform, SocialOAuthProvider> providers =
            new EnumMap<>(SocialPlatform.class);

    public SocialOAuthRegistry(List<SocialOAuthProvider> all) {
        all.forEach(p -> providers.put(p.platform(), p));
    }

    public SocialOAuthProvider resolve(SocialPlatform platform) {
        SocialOAuthProvider provider = providers.get(platform);
        if (provider == null) {
            // platform supported for publishing, but OAuth not yet wired
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{platform + " OAuth is not available yet"});
        }
        return provider;
    }

    public boolean supports(SocialPlatform platform) {
        return providers.containsKey(platform);
    }
}

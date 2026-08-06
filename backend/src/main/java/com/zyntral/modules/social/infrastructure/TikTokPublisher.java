package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

/** TikTok adapter. Real impl: TikTok Content Posting API. */
@Component
public class TikTokPublisher extends AbstractSocialPublisher {
    @Override public SocialPlatform platform() { return SocialPlatform.TIKTOK; }
}

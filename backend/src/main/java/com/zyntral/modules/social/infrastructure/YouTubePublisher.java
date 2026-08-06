package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

/** YouTube adapter. Real impl: YouTube Data API (community posts / video description). */
@Component
public class YouTubePublisher extends AbstractSocialPublisher {
    @Override public SocialPlatform platform() { return SocialPlatform.YOUTUBE; }
}

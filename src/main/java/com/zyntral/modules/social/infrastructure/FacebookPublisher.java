package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

/** Facebook adapter. Real impl: Meta Graph API /{page-id}/feed. */
@Component
public class FacebookPublisher extends AbstractSocialPublisher {
    @Override public SocialPlatform platform() { return SocialPlatform.FACEBOOK; }
}

package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

/** Instagram adapter. Real impl: Meta Graph API content publishing (containers + publish). */
@Component
public class InstagramPublisher extends AbstractSocialPublisher {
    @Override public SocialPlatform platform() { return SocialPlatform.INSTAGRAM; }
}

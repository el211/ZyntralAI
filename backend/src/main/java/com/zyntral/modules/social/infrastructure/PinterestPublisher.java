package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

/** Pinterest adapter. Real impl: Pinterest API create-pin. */
@Component
public class PinterestPublisher extends AbstractSocialPublisher {
    @Override public SocialPlatform platform() { return SocialPlatform.PINTEREST; }
}

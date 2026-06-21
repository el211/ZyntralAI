package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;

/**
 * Instagram OAuth — uses Facebook Login (Instagram Graph API). Connecting links the Meta user;
 * the IG business account (required for publishing) is resolved from the linked page.
 */
@Component
public class InstagramOAuthProvider extends FacebookOAuthProvider {

    private static final String SCOPES =
            "instagram_basic,pages_show_list,instagram_content_publish,public_profile";

    private final SocialOAuthProperties.Provider igConfig;

    public InstagramOAuthProvider(SocialOAuthProperties props) {
        super(props);
        this.igConfig = props.instagram();
    }

    @Override public SocialPlatform platform() { return SocialPlatform.INSTAGRAM; }
    @Override protected String scopes() { return SCOPES; }
    @Override protected SocialOAuthProperties.Provider cfg() { return igConfig; }
}

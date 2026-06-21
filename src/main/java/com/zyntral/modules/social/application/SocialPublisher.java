package com.zyntral.modules.social.application;

import com.zyntral.modules.social.domain.SocialAccount;
import com.zyntral.modules.social.domain.SocialPlatform;

import java.util.List;

/**
 * Port for publishing a post to one social platform. One adapter per platform; the content
 * module fans a post out to N publishers without knowing platform specifics. Adding a network
 * is a new adapter implementing this interface — nothing else changes.
 */
public interface SocialPublisher {

    SocialPlatform platform();

    PublishResult publish(PublishContext context);

    record PublishContext(SocialAccount account, String body, List<String> mediaUrls) {}

    record PublishResult(boolean success, String externalPostId, String permalink, String error) {
        public static PublishResult ok(String externalPostId, String permalink) {
            return new PublishResult(true, externalPostId, permalink, null);
        }
        public static PublishResult failed(String error) {
            return new PublishResult(false, null, null, error);
        }
    }
}

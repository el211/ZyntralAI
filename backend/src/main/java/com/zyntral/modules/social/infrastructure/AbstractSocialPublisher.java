package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.application.SocialPublisher;
import com.zyntral.modules.social.domain.SocialAccountStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Base for platform adapters. Provides validation (character limits, connection status) and a
 * simulated publish so the scheduling/queue pipeline is fully exercised end-to-end. Each real
 * adapter overrides {@link #doPublish} with the platform's actual API call.
 *
 * <p>The OAuth token exchange and HTTP calls are intentionally stubbed — wiring the live APIs
 * (LinkedIn UGC, X v2, Meta Graph, TikTok, YouTube, Pinterest) is per-platform integration work
 * that slots in here without touching the content module.
 */
public abstract class AbstractSocialPublisher implements SocialPublisher {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public PublishResult publish(PublishContext context) {
        if (context.account().getStatus() != SocialAccountStatus.CONNECTED) {
            return PublishResult.failed("Social account is not connected (" +
                    context.account().getStatus() + ")");
        }
        int limit = platform().characterLimit();
        if (limit > 0 && context.body() != null && context.body().length() > limit) {
            return PublishResult.failed("Content exceeds " + platform() + " limit of " + limit
                    + " characters");
        }
        try {
            return doPublish(context);
        } catch (RuntimeException ex) {
            log.error("Publish to {} failed for account {}", platform(),
                    context.account().getId(), ex);
            return PublishResult.failed(ex.getMessage());
        }
    }

    /** Real adapters override with the platform API call. Default: simulated success. */
    protected PublishResult doPublish(PublishContext context) {
        String externalId = platform().name().toLowerCase() + "_" +
                UUID.randomUUID().toString().substring(0, 12);
        String handle = context.account().getHandle();
        String permalink = "https://" + platform().name().toLowerCase() + ".example/"
                + (handle == null ? "post" : handle) + "/" + externalId;
        log.info("[SIMULATED] Published to {} ({} chars) → {}",
                platform(), context.body() == null ? 0 : context.body().length(), externalId);
        return PublishResult.ok(externalId, permalink);
    }
}

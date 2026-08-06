package com.zyntral.modules.content.application;

import com.zyntral.modules.content.domain.Post;
import com.zyntral.modules.content.domain.PostRepository;
import com.zyntral.modules.content.domain.PostTarget;
import com.zyntral.modules.social.application.SocialPublisher;
import com.zyntral.modules.social.application.SocialPublisherRegistry;
import com.zyntral.modules.social.domain.SocialAccount;
import com.zyntral.modules.social.domain.SocialAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Publishes a post to each of its platform targets and settles the post status. Runs on the
 * async worker pool (never a request thread). Each target succeeds or fails independently, so
 * one bad platform doesn't sink the rest.
 */
@Service
public class PostPublishingService {

    private static final Logger log = LoggerFactory.getLogger(PostPublishingService.class);

    private final PostRepository posts;
    private final SocialAccountRepository accounts;
    private final SocialPublisherRegistry publishers;

    public PostPublishingService(PostRepository posts, SocialAccountRepository accounts,
                                 SocialPublisherRegistry publishers) {
        this.posts = posts;
        this.accounts = accounts;
        this.publishers = publishers;
    }

    @Async("taskExecutor")
    @Transactional
    public void publishAsync(UUID postId) {
        publish(postId);
    }

    @Transactional
    public void publish(UUID postId) {
        Post post = posts.findById(postId).orElse(null);
        if (post == null) return;

        post.markPublishing();
        List<String> mediaUrls = post.getMedia().stream().map(m -> m.getUrl()).toList();

        for (PostTarget target : post.getTargets()) {
            target.markPublishing();
            SocialAccount account = accounts.findById(target.getSocialAccountId()).orElse(null);
            if (account == null) {
                target.markFailed("Connected account no longer exists");
                continue;
            }
            try {
                SocialPublisher.PublishResult result = publishers
                        .forPlatform(account.getPlatform())
                        .publish(new SocialPublisher.PublishContext(account, post.getBody(), mediaUrls));
                if (result.success()) {
                    target.markPublished(result.externalPostId(), result.permalink());
                } else {
                    target.markFailed(result.error());
                }
            } catch (RuntimeException ex) {
                log.error("Publish failed for post {} target {}", postId, target.getId(), ex);
                target.markFailed(ex.getMessage());
            }
        }
        post.settlePublishResult();
        log.info("Published post {} → status {}", postId, post.getStatus());
    }
}

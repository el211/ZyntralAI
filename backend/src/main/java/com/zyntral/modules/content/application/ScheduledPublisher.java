package com.zyntral.modules.content.application;

import com.zyntral.modules.content.domain.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Polls for posts whose scheduled time has arrived and dispatches each to the async publisher.
 * Backed by the partial index {@code idx_posts_due}. Dispatch hands off immediately, so a slow
 * platform never stalls the scan.
 *
 * <p>At multi-instance scale this scan should hold a short advisory lock (or move to an
 * outbox/queue) so only one instance claims a batch; noted for the clustered deployment.
 */
@Component
public class ScheduledPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPublisher.class);
    private static final int BATCH = 50;

    private final PostRepository posts;
    private final PostPublishingService publishing;

    public ScheduledPublisher(PostRepository posts, PostPublishingService publishing) {
        this.posts = posts;
        this.publishing = publishing;
    }

    @Scheduled(fixedDelayString = "${zyntral.scheduler.publish-interval-ms:30000}")
    public void publishDuePosts() {
        List<UUID> due = posts.findDuePostIds(Instant.now(), PageRequest.of(0, BATCH));
        if (due.isEmpty()) return;
        log.info("Dispatching {} due post(s)", due.size());
        due.forEach(publishing::publishAsync);
    }
}

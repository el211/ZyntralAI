package com.zyntral.modules.content.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispatches an immediate-publish post to the async worker only after the queuing transaction
 * has committed — so the worker always sees the persisted QUEUED post and its targets.
 */
@Component
public class PostPublishDispatcher {

    private final PostPublishingService publishing;

    public PostPublishDispatcher(PostPublishingService publishing) {
        this.publishing = publishing;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PostQueuedEvent event) {
        publishing.publishAsync(event.postId());
    }
}

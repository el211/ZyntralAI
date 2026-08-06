package com.zyntral.modules.content.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostTest {

    private Post draft() {
        return Post.draft(UUID.randomUUID(), UUID.randomUUID(), "Title", "Body", null);
    }

    @Test
    void newPostIsDraft() {
        assertThat(draft().getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void cannotScheduleWithoutTargets() {
        Post post = draft();
        assertThatThrownBy(() -> post.schedule(Instant.now().plus(1, ChronoUnit.HOURS)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotScheduleInThePast() {
        Post post = draft();
        post.retarget(List.of(UUID.randomUUID()));
        assertThatThrownBy(() -> post.schedule(Instant.now().minusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schedulesIntoTheFuture() {
        Post post = draft();
        post.retarget(List.of(UUID.randomUUID()));
        post.schedule(Instant.now().plus(2, ChronoUnit.HOURS));
        assertThat(post.getStatus()).isEqualTo(PostStatus.SCHEDULED);
        assertThat(post.getTargets()).hasSize(1);
    }

    @Test
    void queueNowMarksQueued() {
        Post post = draft();
        post.retarget(List.of(UUID.randomUUID()));
        post.queueNow();
        assertThat(post.getStatus()).isEqualTo(PostStatus.QUEUED);
    }

    @Test
    void settleMarksPublishedWhenAnyTargetSucceeds() {
        Post post = draft();
        post.retarget(List.of(UUID.randomUUID()));
        post.markPublishing();
        post.getTargets().get(0).markPublished("ext_1", "https://x/1");
        post.settlePublishResult();
        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isNotNull();
    }

    @Test
    void cannotEditAfterPublished() {
        Post post = draft();
        post.retarget(List.of(UUID.randomUUID()));
        post.markPublishing();
        post.getTargets().get(0).markPublished("ext_1", "https://x/1");
        post.settlePublishResult();
        assertThatThrownBy(() -> post.edit("new", "new")).isInstanceOf(IllegalStateException.class);
    }
}

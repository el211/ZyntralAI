package com.zyntral.modules.content.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.content.domain.Post;
import com.zyntral.modules.content.domain.PostRepository;
import com.zyntral.modules.content.domain.PostStatus;
import com.zyntral.modules.content.web.dto.ContentDtos.*;
import com.zyntral.modules.social.domain.SocialAccountRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Post authoring and lifecycle: drafts, scheduling, immediate publish, calendar, and queue.
 * Editing requires EDITOR+; reads require membership. Target accounts are validated to belong
 * to the workspace, preventing cross-tenant publishing.
 */
@Service
public class PostService {

    private final PostRepository posts;
    private final SocialAccountRepository accounts;
    private final WorkspaceAccess access;
    private final ApplicationEventPublisher events;

    public PostService(PostRepository posts, SocialAccountRepository accounts,
                       WorkspaceAccess access, ApplicationEventPublisher events) {
        this.posts = posts;
        this.accounts = accounts;
        this.access = access;
        this.events = events;
    }

    @Transactional
    public PostResponse create(UUID workspaceId, UUID userId, CreatePostRequest req) {
        access.requireCanEdit(workspaceId, userId);
        validateTargets(workspaceId, req.socialAccountIds());

        Post post = Post.draft(workspaceId, userId, req.title(), req.body(), req.aiGenerationId());
        post.retarget(req.socialAccountIds());
        if (req.media() != null) {
            req.media().forEach(m -> post.addMedia(m.url(), m.mediaType(), m.altText()));
        }
        return toResponse(posts.save(post));
    }

    @Transactional
    public PostResponse update(UUID workspaceId, UUID userId, UUID postId, UpdatePostRequest req) {
        access.requireCanEdit(workspaceId, userId);
        Post post = load(workspaceId, postId);
        post.edit(req.title(), req.body());
        if (req.socialAccountIds() != null && !req.socialAccountIds().isEmpty()) {
            validateTargets(workspaceId, req.socialAccountIds());
            post.retarget(req.socialAccountIds());
        }
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public PostResponse get(UUID workspaceId, UUID userId, UUID postId) {
        access.requireMember(workspaceId, userId);
        return toResponse(load(workspaceId, postId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(UUID workspaceId, UUID userId, PostStatus status,
                                           int page, int size) {
        access.requireMember(workspaceId, userId);
        var pageable = PageRequest.of(page, size);
        var result = (status == null)
                ? posts.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
                : posts.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, status, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> calendar(UUID workspaceId, UUID userId, Instant from, Instant to) {
        access.requireMember(workspaceId, userId);
        return posts.findCalendar(workspaceId, from, to).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PostResponse schedule(UUID workspaceId, UUID userId, UUID postId, Instant when) {
        access.requireCanEdit(workspaceId, userId);
        Post post = load(workspaceId, postId);
        try {
            post.schedule(when);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{e.getMessage()});
        }
        return toResponse(post);
    }

    @Transactional
    public PostResponse publishNow(UUID workspaceId, UUID userId, UUID postId) {
        access.requireCanEdit(workspaceId, userId);
        Post post = load(workspaceId, postId);
        try {
            post.queueNow();
        } catch (IllegalStateException e) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{e.getMessage()});
        }
        // dispatch happens AFTER_COMMIT (see PostPublishDispatcher) so the worker sees it persisted
        events.publishEvent(new PostQueuedEvent(post.getId()));
        return toResponse(post);
    }

    @Transactional
    public PostResponse cancel(UUID workspaceId, UUID userId, UUID postId) {
        access.requireCanEdit(workspaceId, userId);
        Post post = load(workspaceId, postId);
        try {
            post.cancel();
        } catch (IllegalStateException e) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{e.getMessage()});
        }
        return toResponse(post);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID userId, UUID postId) {
        access.requireCanEdit(workspaceId, userId);
        posts.delete(load(workspaceId, postId));
    }

    // ---- helpers ----

    private void validateTargets(UUID workspaceId, List<UUID> socialAccountIds) {
        for (UUID accountId : socialAccountIds) {
            accounts.findByIdAndWorkspaceId(accountId, workspaceId)
                    .orElseThrow(() -> ApiException.notFound("social account", accountId));
        }
    }

    private Post load(UUID workspaceId, UUID postId) {
        return posts.findByIdAndWorkspaceId(postId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("post", postId));
    }

    private PostResponse toResponse(Post p) {
        List<TargetResponse> targets = p.getTargets().stream()
                .map(t -> new TargetResponse(t.getId(), t.getSocialAccountId(), t.getStatus(),
                        t.getPermalink(), t.getErrorMessage()))
                .toList();
        List<MediaResponse> media = p.getMedia().stream()
                .map(m -> new MediaResponse(m.getUrl(), m.getMediaType(), m.getAltText()))
                .toList();
        return new PostResponse(p.getId(), p.getTitle(), p.getBody(), p.getStatus(),
                p.getScheduledAt(), p.getPublishedAt(), targets, media, p.getCreatedAt());
    }
}

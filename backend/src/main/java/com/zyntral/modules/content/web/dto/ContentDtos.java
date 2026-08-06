package com.zyntral.modules.content.web.dto;

import com.zyntral.modules.content.domain.PostStatus;
import com.zyntral.modules.content.domain.TargetStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContentDtos {

    private ContentDtos() {}

    public record MediaItem(String url, String mediaType, String altText) {}

    public record CreatePostRequest(
            @Size(max = 200) String title,
            @Size(max = 20000) String body,
            @NotEmpty List<UUID> socialAccountIds,
            List<MediaItem> media,
            UUID aiGenerationId
    ) {}

    public record UpdatePostRequest(
            @Size(max = 200) String title,
            @Size(max = 20000) String body,
            List<UUID> socialAccountIds
    ) {}

    public record SchedulePostRequest(@NotNull Instant scheduledAt) {}

    public record TargetResponse(UUID id, UUID socialAccountId, TargetStatus status,
                                 String permalink, String error) {}

    public record MediaResponse(String url, String mediaType, String altText) {}

    public record PostResponse(
            UUID id, String title, String body, PostStatus status,
            Instant scheduledAt, Instant publishedAt,
            List<TargetResponse> targets, List<MediaResponse> media, Instant createdAt
    ) {}
}

package com.zyntral.modules.dubbing.web.dto;

import com.zyntral.modules.dubbing.domain.DubbingJob;
import com.zyntral.modules.dubbing.domain.DubbingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class DubbingDtos {

    private DubbingDtos() {}

    public record SaveCredentialRequest(@NotBlank @Size(max = 500) String apiKey) {}

    public record CredentialStatusResponse(boolean configured) {}

    public record DubbingJobResponse(
            UUID id, String name, String sourceLang, String targetLang,
            DubbingStatus status, String error, Instant createdAt
    ) {
        public static DubbingJobResponse from(DubbingJob j) {
            return new DubbingJobResponse(j.getId(), j.getName(), j.getSourceLang(),
                    j.getTargetLang(), j.getStatus(), j.getError(), j.getCreatedAt());
        }
    }
}

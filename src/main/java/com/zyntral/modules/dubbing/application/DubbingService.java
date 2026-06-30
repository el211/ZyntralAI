package com.zyntral.modules.dubbing.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.ai.application.TtsService;
import com.zyntral.modules.ai.domain.WorkspaceProviderKey;
import com.zyntral.modules.ai.domain.WorkspaceProviderKeyRepository;
import com.zyntral.modules.dubbing.domain.*;
import com.zyntral.modules.dubbing.infrastructure.ElevenLabsDubbingClient;
import com.zyntral.modules.dubbing.infrastructure.ElevenLabsDubbingClient.DownloadedMedia;
import com.zyntral.modules.dubbing.infrastructure.ElevenLabsDubbingClient.DubStatus;
import com.zyntral.modules.dubbing.web.dto.DubbingDtos.DubbingJobResponse;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates video dubbing through ElevenLabs using each workspace's own API key (BYOK):
 * authorize → load the workspace key → call ElevenLabs → record/refresh job metadata.
 * Like {@code AiGenerationService}, the external HTTP call happens outside any DB transaction.
 *
 * <p>The ElevenLabs key is the same per-workspace BYOK key used by {@link TtsService} (provider
 * {@code "ELEVENLABS"} in {@code workspace_provider_keys}), so a workspace sets it once for TTS,
 * voice cloning, and dubbing alike.
 */
@Service
public class DubbingService {

    /** Same provider slug TTS/voice-cloning use, so the ElevenLabs key is shared. */
    private static final String PROVIDER = TtsService.PROVIDER;

    private final WorkspaceAccess access;
    private final WorkspaceProviderKeyRepository providerKeys;
    private final DubbingJobRepository jobs;
    private final ElevenLabsDubbingClient client;

    public DubbingService(WorkspaceAccess access, WorkspaceProviderKeyRepository providerKeys,
                          DubbingJobRepository jobs, ElevenLabsDubbingClient client) {
        this.access = access;
        this.providerKeys = providerKeys;
        this.jobs = jobs;
        this.client = client;
    }

    // ---- credentials -------------------------------------------------------

    @Transactional
    public void saveCredential(UUID workspaceId, UUID userId, String apiKey) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.business(ErrorCode.BUSINESS_RULE);
        }
        WorkspaceProviderKey existing = providerKeys.find(workspaceId, PROVIDER).orElse(null);
        if (existing != null) {
            existing.setApiKey(apiKey.trim());
        } else {
            providerKeys.save(new WorkspaceProviderKey(workspaceId, PROVIDER, apiKey.trim()));
        }
    }

    @Transactional(readOnly = true)
    public boolean hasCredential(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return providerKeys.find(workspaceId, PROVIDER).isPresent();
    }

    // ---- dubbing -----------------------------------------------------------

    /** Submit a video for dubbing. {@code sourceLang} may be null/blank to auto-detect. */
    public DubbingJobResponse startDub(UUID workspaceId, UUID userId, byte[] media, String filename,
                                       String contentType, String targetLang, String sourceLang,
                                       String name) {
        access.requireCanEdit(workspaceId, userId);
        if (media == null || media.length == 0) {
            throw ApiException.business(ErrorCode.BUSINESS_RULE);
        }
        String apiKey = requireKey(workspaceId);

        var created = client.createDub(apiKey, media, filename, contentType, targetLang,
                blankToNull(sourceLang), name);

        DubbingJob job = DubbingJob.create(workspaceId, userId, created.dubbingId(),
                blankToNull(name), blankToNull(sourceLang), targetLang, DubbingStatus.DUBBING);
        jobs.save(job);
        return DubbingJobResponse.from(job);
    }

    /** Poll ElevenLabs for the latest status of a job and persist the change. */
    public DubbingJobResponse refreshStatus(UUID workspaceId, UUID userId, UUID jobId) {
        access.requireMember(workspaceId, userId);
        DubbingJob job = loadJob(workspaceId, jobId);

        // Terminal states never change again — no need to call out.
        if (job.getStatus() != DubbingStatus.DUBBED && job.getStatus() != DubbingStatus.FAILED) {
            String apiKey = requireKey(workspaceId);
            DubStatus status = client.getStatus(apiKey, job.getDubbingId());
            if (status.status() != job.getStatus() || status.error() != null) {
                job.updateStatus(status.status(), status.error());
                jobs.save(job); // repository save is itself transactional
            }
        }
        return DubbingJobResponse.from(job);
    }

    /** Stream the rendered dubbed media (in the job's target language). */
    @Transactional(readOnly = true)
    public DownloadedMedia download(UUID workspaceId, UUID userId, UUID jobId) {
        access.requireMember(workspaceId, userId);
        DubbingJob job = loadJob(workspaceId, jobId);
        if (job.getStatus() != DubbingStatus.DUBBED) {
            throw ApiException.business(ErrorCode.BUSINESS_RULE);
        }
        String apiKey = requireKey(workspaceId);
        return client.download(apiKey, job.getDubbingId(), job.getTargetLang());
    }

    @Transactional(readOnly = true)
    public PageResponse<DubbingJobResponse> history(UUID workspaceId, UUID userId, int page, int size) {
        access.requireMember(workspaceId, userId);
        var result = jobs.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(page, size))
                .map(DubbingJobResponse::from);
        return PageResponse.from(result);
    }

    // ---- helpers -----------------------------------------------------------

    private String requireKey(UUID workspaceId) {
        return providerKeys.find(workspaceId, PROVIDER)
                .map(WorkspaceProviderKey::getApiKey)
                .filter(k -> k != null && !k.isBlank())
                .orElseThrow(() -> ApiException.business(ErrorCode.BUSINESS_RULE));
    }

    private DubbingJob loadJob(UUID workspaceId, UUID jobId) {
        return jobs.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("DubbingJob", jobId));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

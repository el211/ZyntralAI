package com.zyntral.modules.ai.application;

import com.zyntral.modules.ai.domain.AiProviderKind;
import com.zyntral.modules.ai.domain.WorkspaceProviderKey;
import com.zyntral.modules.ai.domain.WorkspaceProviderKeyRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Manages workspace BYOK (Bring-Your-Own-Key) API keys for external AI providers. */
@Service
public class ProviderKeyService {

    private final WorkspaceProviderKeyRepository repo;
    private final WorkspaceAccess access;

    public ProviderKeyService(WorkspaceProviderKeyRepository repo, WorkspaceAccess access) {
        this.repo = repo;
        this.access = access;
    }

    public record ProviderStatus(String provider, boolean configured) {}

    /** Returns which providers have a key configured — never returns the actual key. */
    @Transactional(readOnly = true)
    public List<ProviderStatus> listStatuses(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return Arrays.stream(AiProviderKind.values())
                .map(kind -> new ProviderStatus(
                        kind.name(),
                        repo.find(workspaceId, kind.name()).isPresent()))
                .toList();
    }

    /** Saves (or replaces) a workspace API key for the given provider. Key is encrypted at rest. */
    @Transactional
    public void save(UUID workspaceId, UUID userId, String provider, String apiKey) {
        access.requireCanEdit(workspaceId, userId);
        validateProvider(provider);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be blank");
        }
        // Upsert
        WorkspaceProviderKey key = repo.find(workspaceId, provider.toUpperCase())
                .orElse(new WorkspaceProviderKey(workspaceId, provider.toUpperCase(), apiKey));
        key.setApiKey(apiKey);
        repo.save(key);
    }

    /** Removes the workspace's stored API key for the given provider. */
    @Transactional
    public void delete(UUID workspaceId, UUID userId, String provider) {
        access.requireCanEdit(workspaceId, userId);
        validateProvider(provider);
        repo.remove(workspaceId, provider.toUpperCase());
    }

    private void validateProvider(String provider) {
        try {
            AiProviderKind.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }
}

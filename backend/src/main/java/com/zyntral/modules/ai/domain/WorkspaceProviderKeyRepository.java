package com.zyntral.modules.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceProviderKeyRepository
        extends JpaRepository<WorkspaceProviderKey, WorkspaceProviderKey.Id> {

    default Optional<WorkspaceProviderKey> find(UUID workspaceId, String provider) {
        return findById(new WorkspaceProviderKey.Id(workspaceId, provider));
    }

    default void remove(UUID workspaceId, String provider) {
        deleteById(new WorkspaceProviderKey.Id(workspaceId, provider));
    }
}

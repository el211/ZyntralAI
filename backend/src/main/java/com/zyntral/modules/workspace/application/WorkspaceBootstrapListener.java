package com.zyntral.modules.workspace.application;

import com.zyntral.common.event.UserEmailVerifiedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Creates a personal workspace once a user verifies their email. Runs after the
 * verification transaction commits so it never blocks or rolls back verification.
 */
@Component
public class WorkspaceBootstrapListener {

    private final WorkspaceService workspaces;

    public WorkspaceBootstrapListener(WorkspaceService workspaces) {
        this.workspaces = workspaces;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserEmailVerifiedEvent event) {
        workspaces.bootstrapForUser(event.userId(), event.fullName(), event.email());
    }
}

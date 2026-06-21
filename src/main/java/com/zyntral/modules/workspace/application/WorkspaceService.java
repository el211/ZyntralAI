package com.zyntral.modules.workspace.application;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.auth.application.EmailSender;
import com.zyntral.modules.auth.application.TokenGenerator;
import com.zyntral.modules.billing.domain.Plan;
import com.zyntral.modules.billing.domain.PlanRepository;
import com.zyntral.modules.user.domain.User;
import com.zyntral.modules.user.domain.UserRepository;
import com.zyntral.modules.workspace.domain.*;
import com.zyntral.modules.workspace.web.dto.WorkspaceDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Workspace lifecycle and membership. Enforces per-plan limits (max workspaces, max team
 * members) and keeps invitations single-use and hashed. All cross-tenant reads go through
 * {@link WorkspaceAccess}.
 */
@Service
public class WorkspaceService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final WorkspaceInvitationRepository invitations;
    private final PlanRepository plans;
    private final UserRepository users;
    private final WorkspaceAccess access;
    private final TokenGenerator tokens;
    private final EmailSender email;

    public WorkspaceService(WorkspaceRepository workspaces, WorkspaceMemberRepository members,
                            WorkspaceInvitationRepository invitations, PlanRepository plans,
                            UserRepository users, WorkspaceAccess access,
                            TokenGenerator tokens, EmailSender email) {
        this.workspaces = workspaces;
        this.members = members;
        this.invitations = invitations;
        this.plans = plans;
        this.users = users;
        this.access = access;
        this.tokens = tokens;
        this.email = email;
    }

    // ---- creation ----

    @Transactional
    public WorkspaceResponse create(UUID userId, CreateWorkspaceRequest req) {
        enforceWorkspaceLimit(userId);
        Workspace ws = persistWithOwner(req.name(), userId);
        return toResponse(ws, userId);
    }

    /** Called after email verification to give the user a ready-to-use personal workspace. */
    @Transactional
    public void bootstrapForUser(UUID userId, String fullName, String email) {
        if (!workspaces.findAllForUser(userId).isEmpty()) {
            return; // idempotent — already has a workspace
        }
        String name = (fullName != null && !fullName.isBlank())
                ? fullName.split(" ")[0] + "'s Workspace"
                : email.split("@")[0] + "'s Workspace";
        persistWithOwner(name, userId);
    }

    private Workspace persistWithOwner(String name, UUID ownerId) {
        Workspace ws = workspaces.save(Workspace.create(name, uniqueSlug(name), ownerId));
        members.save(WorkspaceMember.of(ws.getId(), ownerId, WorkspaceRole.OWNER));
        return ws;
    }

    // ---- reads ----

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(UUID userId) {
        return workspaces.findAllForUser(userId).stream()
                .map(ws -> toResponse(ws, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return toResponse(load(workspaceId), userId);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return members.findByIdWorkspaceId(workspaceId).stream()
                .map(m -> new MemberResponse(m.getUserId(), m.getRole(), m.getJoinedAt()))
                .toList();
    }

    // ---- updates ----

    @Transactional
    public WorkspaceResponse update(UUID workspaceId, UUID userId, UpdateWorkspaceRequest req) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        Workspace ws = load(workspaceId);
        if (req.name() != null && !req.name().isBlank()) ws.rename(req.name());
        if (req.imageUrl() != null) ws.setImageUrl(req.imageUrl());
        return toResponse(ws, userId);
    }

    // ---- invitations ----

    @Transactional
    public void invite(UUID workspaceId, UUID actorId, InviteMemberRequest req) {
        access.requireCanManageMembers(workspaceId, actorId);
        Workspace ws = load(workspaceId);
        enforceMemberLimit(ws);
        String inviteeEmail = com.zyntral.modules.user.domain.User.normalizeEmail(req.email());
        if (invitations.existsByWorkspaceIdAndEmailAndStatus(
                workspaceId, inviteeEmail, InvitationStatus.PENDING)) {
            throw new ApiException(ErrorCode.CONFLICT);
        }
        var secret = tokens.generate();
        invitations.save(WorkspaceInvitation.create(workspaceId, inviteeEmail, req.role(),
                secret.hash(), actorId, Instant.now().plusSeconds(7L * 86_400)));
        email.sendWorkspaceInvitation(inviteeEmail, ws.getName(), secret.raw());
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listInvitations(UUID workspaceId, UUID userId) {
        access.requireCanManageMembers(workspaceId, userId);
        return invitations.findByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING).stream()
                .map(i -> new InvitationResponse(i.getId(), i.getEmail(), i.getRole(),
                        i.getStatus(), i.getExpiresAt()))
                .toList();
    }

    @Transactional
    public WorkspaceResponse acceptInvitation(UUID userId, String rawToken) {
        WorkspaceInvitation inv = invitations.findByTokenHash(tokens.hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        if (!inv.isAcceptable()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        User user = users.findById(userId).orElseThrow(ApiException::forbidden);
        if (!user.getEmail().equalsIgnoreCase(inv.getEmail())) {
            throw new ApiException(ErrorCode.FORBIDDEN);  // invite was for a different address
        }
        if (!members.existsByIdWorkspaceIdAndIdUserId(inv.getWorkspaceId(), userId)) {
            members.save(WorkspaceMember.of(inv.getWorkspaceId(), userId, inv.getRole()));
        }
        inv.accept();
        return toResponse(load(inv.getWorkspaceId()), userId);
    }

    // ---- member management ----

    @Transactional
    public void updateMemberRole(UUID workspaceId, UUID actorId, UUID targetUserId,
                                 UpdateMemberRoleRequest req) {
        access.requireCanManageMembers(workspaceId, actorId);
        WorkspaceMember member = members
                .findByIdWorkspaceIdAndIdUserId(workspaceId, targetUserId)
                .orElseThrow(() -> ApiException.notFound("member", targetUserId));
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN);  // owner role is not reassignable here
        }
        member.changeRole(req.role());
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID actorId, UUID targetUserId) {
        access.requireCanManageMembers(workspaceId, actorId);
        WorkspaceMember member = members
                .findByIdWorkspaceIdAndIdUserId(workspaceId, targetUserId)
                .orElseThrow(() -> ApiException.notFound("member", targetUserId));
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        members.delete(member);
    }

    @Transactional
    public void leave(UUID workspaceId, UUID userId) {
        WorkspaceMember member = members
                .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
                .orElseThrow(ApiException::forbidden);
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN);  // owner must transfer or delete
        }
        members.delete(member);
    }

    // ---- helpers ----

    private void enforceWorkspaceLimit(UUID userId) {
        PlanCode effective = effectivePlan(userId);
        Plan plan = plans.findById(effective).orElseThrow();
        if (workspaces.countByOwnerId(userId) >= plan.getMaxWorkspaces()) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_REACHED);
        }
    }

    private void enforceMemberLimit(Workspace ws) {
        Plan plan = plans.findById(ws.getPlan()).orElseThrow();
        long current = members.countByIdWorkspaceId(ws.getId());
        long pending = invitations
                .findByWorkspaceIdAndStatus(ws.getId(), InvitationStatus.PENDING).size();
        if (current + pending >= plan.getMaxTeamMembers()) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_REACHED);
        }
    }

    /** A user's effective tier = the highest plan among workspaces they own (default FREE). */
    private PlanCode effectivePlan(UUID userId) {
        return workspaces.findAllForUser(userId).stream()
                .filter(w -> w.getOwnerId().equals(userId))
                .map(Workspace::getPlan)
                .max(java.util.Comparator.comparingInt(Enum::ordinal))
                .orElse(PlanCode.FREE);
    }

    private Workspace load(UUID workspaceId) {
        return workspaces.findById(workspaceId)
                .orElseThrow(() -> ApiException.notFound("workspace", workspaceId));
    }

    private WorkspaceResponse toResponse(Workspace ws, UUID userId) {
        WorkspaceRole myRole = members
                .findByIdWorkspaceIdAndIdUserId(ws.getId(), userId)
                .map(WorkspaceMember::getRole)
                .orElse(null);
        long memberCount = members.countByIdWorkspaceId(ws.getId());
        return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(), ws.getPlan(),
                ws.getImageUrl(), myRole, memberCount, ws.getCreatedAt());
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "workspace";
        String slug;
        do {
            slug = base + "-" + Integer.toHexString(RANDOM.nextInt(0x10000));
        } while (workspaces.existsBySlug(slug));
        return slug;
    }
}

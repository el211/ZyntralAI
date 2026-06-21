package com.zyntral.modules.support.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.modules.support.domain.*;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** CRUD for support agents and their knowledge base (ADMIN+ to manage; members can read). */
@Service
public class SupportAgentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SupportAgentRepository agents;
    private final SupportKnowledgeRepository knowledge;
    private final WorkspaceAccess access;

    public SupportAgentService(SupportAgentRepository agents, SupportKnowledgeRepository knowledge,
                               WorkspaceAccess access) {
        this.agents = agents;
        this.knowledge = knowledge;
        this.access = access;
    }

    @Transactional
    public SupportAgent create(UUID workspaceId, UUID userId, String name, String systemPrompt,
                               String model) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        return agents.save(SupportAgent.create(workspaceId, name, generatePublicKey(),
                systemPrompt, model));
    }

    @Transactional(readOnly = true)
    public List<SupportAgent> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return agents.findByWorkspaceId(workspaceId);
    }

    @Transactional
    public SupportAgent update(UUID workspaceId, UUID userId, UUID agentId, String name,
                               String systemPrompt, String model, Boolean active) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        SupportAgent agent = loadAgent(workspaceId, agentId);
        agent.update(name, systemPrompt, model, active);
        return agent;
    }

    @Transactional
    public void delete(UUID workspaceId, UUID userId, UUID agentId) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        agents.delete(loadAgent(workspaceId, agentId));
    }

    @Transactional
    public SupportKnowledge addKnowledge(UUID workspaceId, UUID userId, UUID agentId, String title,
                                         String content, String sourceUrl) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        loadAgent(workspaceId, agentId);   // ownership check
        return knowledge.save(SupportKnowledge.of(agentId, title, content, sourceUrl));
    }

    @Transactional(readOnly = true)
    public List<SupportKnowledge> listKnowledge(UUID workspaceId, UUID userId, UUID agentId) {
        access.requireMember(workspaceId, userId);
        loadAgent(workspaceId, agentId);
        return knowledge.findByAgentId(agentId);
    }

    @Transactional
    public void deleteKnowledge(UUID workspaceId, UUID userId, UUID agentId, UUID knowledgeId) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        loadAgent(workspaceId, agentId);
        SupportKnowledge k = knowledge.findByIdAndAgentId(knowledgeId, agentId)
                .orElseThrow(() -> ApiException.notFound("knowledge", knowledgeId));
        knowledge.delete(k);
    }

    private SupportAgent loadAgent(UUID workspaceId, UUID agentId) {
        return agents.findByIdAndWorkspaceId(agentId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("support agent", agentId));
    }

    private String generatePublicKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "zyn_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

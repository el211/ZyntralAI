package com.zyntral.modules.support.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.application.AiProvider;
import com.zyntral.modules.ai.application.AiProviderRegistry;
import com.zyntral.modules.support.domain.*;
import com.zyntral.modules.support.domain.SupportRepositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Public chat used by the embeddable widget. Identified by the agent's public key (no auth).
 * Answers are grounded in the agent's knowledge base and the recent conversation history.
 *
 * <p>Being public, this endpoint must be rate-limited at the gateway (Redis token bucket) —
 * noted for the production gateway config.
 */
@Service
public class SupportChatService {

    private static final int HISTORY_LIMIT = 12;

    private final SupportAgentRepository agents;
    private final SupportKnowledgeRepository knowledge;
    private final SupportConversationRepository conversations;
    private final SupportMessageRepository messages;
    private final AiProviderRegistry ai;

    public SupportChatService(SupportAgentRepository agents, SupportKnowledgeRepository knowledge,
                              SupportConversationRepository conversations,
                              SupportMessageRepository messages, AiProviderRegistry ai) {
        this.agents = agents;
        this.knowledge = knowledge;
        this.conversations = conversations;
        this.messages = messages;
        this.ai = ai;
    }

    public record ChatResult(UUID conversationId, String reply) {}

    @Transactional
    public ChatResult chat(String publicKey, String visitorId, UUID conversationId, String message) {
        SupportAgent agent = agents.findByPublicKey(publicKey)
                .filter(SupportAgent::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        SupportConversation conversation = resolveConversation(agent, conversationId, visitorId);
        List<SupportMessage> history = messages.findByConversationIdOrderByCreatedAt(conversation.getId());

        messages.save(SupportMessage.of(conversation.getId(), ConversationRole.USER, message));

        String system = buildSystemPrompt(agent);
        String userPrompt = buildTranscript(history, message);

        AiProvider.AiCompletion completion = ai.resolve(null).complete(
                new AiProvider.AiRequest(system, userPrompt, agent.getModel(), 600, 0.4));

        messages.save(SupportMessage.of(conversation.getId(), ConversationRole.ASSISTANT,
                completion.text()));
        return new ChatResult(conversation.getId(), completion.text());
    }

    private SupportConversation resolveConversation(SupportAgent agent, UUID conversationId,
                                                    String visitorId) {
        if (conversationId != null) {
            return conversations.findById(conversationId)
                    .filter(c -> c.getAgentId().equals(agent.getId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        }
        return conversations.save(SupportConversation.start(agent.getId(), visitorId));
    }

    private String buildSystemPrompt(SupportAgent agent) {
        StringBuilder sb = new StringBuilder();
        sb.append(agent.getSystemPrompt() == null
                ? "You are a helpful customer-support assistant." : agent.getSystemPrompt());
        List<SupportKnowledge> kb = knowledge.findByAgentId(agent.getId());
        if (!kb.isEmpty()) {
            sb.append("\n\nUse the following knowledge base to answer. If the answer isn't here, ")
              .append("say you're not sure and offer to connect the user with a human.\n");
            kb.forEach(k -> sb.append("\n## ").append(k.getTitle() == null ? "Note" : k.getTitle())
                    .append("\n").append(k.getContent()).append("\n"));
        }
        return sb.toString();
    }

    private String buildTranscript(List<SupportMessage> history, String latest) {
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - HISTORY_LIMIT);
        for (int i = start; i < history.size(); i++) {
            SupportMessage m = history.get(i);
            sb.append(m.getRole() == ConversationRole.USER ? "User: " : "Assistant: ")
              .append(m.getContent()).append("\n");
        }
        sb.append("User: ").append(latest).append("\nAssistant:");
        return sb.toString();
    }
}

package com.zyntral.modules.support.web.dto;

import com.zyntral.modules.support.domain.SupportAgent;
import com.zyntral.modules.support.domain.SupportKnowledge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class SupportDtos {

    private SupportDtos() {}

    public record CreateAgentRequest(@NotBlank @Size(max = 140) String name,
                                     String systemPrompt, String model) {}

    public record UpdateAgentRequest(String name, String systemPrompt, String model, Boolean active) {}

    public record AgentResponse(UUID id, String name, String publicKey, String systemPrompt,
                                String model, boolean active, Instant createdAt) {
        public static AgentResponse from(SupportAgent a) {
            return new AgentResponse(a.getId(), a.getName(), a.getPublicKey(), a.getSystemPrompt(),
                    a.getModel(), a.isActive(), a.getCreatedAt());
        }
    }

    public record AddKnowledgeRequest(String title, @NotBlank String content, String sourceUrl) {}

    public record KnowledgeResponse(UUID id, String title, String content, String sourceUrl,
                                    Instant createdAt) {
        public static KnowledgeResponse from(SupportKnowledge k) {
            return new KnowledgeResponse(k.getId(), k.getTitle(), k.getContent(), k.getSourceUrl(),
                    k.getCreatedAt());
        }
    }

    public record ChatRequest(@NotBlank String publicKey, @NotBlank String message,
                              String visitorId, UUID conversationId) {}

    public record ChatResponse(UUID conversationId, String reply) {}
}

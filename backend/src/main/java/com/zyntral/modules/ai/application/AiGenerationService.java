package com.zyntral.modules.ai.application;

import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.ai.domain.*;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates an AI generation: authorize → charge credits → call the provider → record.
 * The provider HTTP call happens <em>outside</em> any DB transaction (no connection held
 * across the network round-trip); credits are refunded if the call fails.
 */
@Service
public class AiGenerationService {

    private final WorkspaceAccess access;
    private final AiCreditService credits;
    private final AiProviderRegistry registry;
    private final PromptBuilder prompts;
    private final AiGenerationRepository generations;

    public AiGenerationService(WorkspaceAccess access, AiCreditService credits,
                               AiProviderRegistry registry, PromptBuilder prompts,
                               AiGenerationRepository generations) {
        this.access = access;
        this.credits = credits;
        this.registry = registry;
        this.prompts = prompts;
        this.generations = generations;
    }

    public record GenerateCommand(
            UUID workspaceId, UUID userId,
            AiContentKind contentKind, AiTone tone, AiLength length, String language,
            String topic, String extraContext,
            AiProviderKind provider, String model
    ) {}

    public record GenerationResult(
            UUID id, String output, AiProviderKind provider, String model,
            int promptTokens, int outputTokens, int creditsCost
    ) {}

    public GenerationResult generate(GenerateCommand cmd) {
        access.requireCanEdit(cmd.workspaceId(), cmd.userId());

        AiLength length = cmd.length() != null ? cmd.length() : AiLength.MEDIUM;
        int cost = creditCost(length);

        // 1. charge first (own tx) so we never overspend under concurrency
        credits.charge(cmd.workspaceId(), cost);

        try {
            // 2. call the provider OUTSIDE any DB transaction
            AiProvider provider = registry.resolve(cmd.provider());
            PromptBuilder.Prompt prompt = prompts.build(cmd.contentKind(), cmd.tone(), length,
                    cmd.language(), cmd.topic(), cmd.extraContext());

            long start = System.currentTimeMillis();
            AiProvider.AiCompletion completion = provider.complete(new AiProvider.AiRequest(
                    prompt.system(), prompt.user(), cmd.model(), length.maxTokens(), 0.7));
            int latency = (int) (System.currentTimeMillis() - start);

            // 3. persist the record (own tx)
            return record(cmd, provider.kind(), length, cost, prompt.user(), completion, latency);
        } catch (RuntimeException ex) {
            // provider failed after charging → give the credits back
            credits.refund(cmd.workspaceId(), cost);
            throw ex;
        }
    }

    // Not @Transactional: called via internal self-invocation (proxy wouldn't apply it).
    // repository.save() is itself transactional, which is all we need here.
    private GenerationResult record(GenerateCommand cmd, AiProviderKind providerKind,
                                    AiLength length, int cost, String userPrompt,
                                    AiProvider.AiCompletion completion, int latency) {
        AiGeneration g = AiGeneration.success(
                cmd.workspaceId(), cmd.userId(), providerKind, completion.model(),
                cmd.contentKind(), cmd.tone(), length,
                cmd.language() == null ? "en" : cmd.language(),
                userPrompt, completion.text(),
                completion.promptTokens(), completion.outputTokens(), cost, latency);
        generations.save(g);
        return new GenerationResult(g.getId(), completion.text(), providerKind, completion.model(),
                completion.promptTokens(), completion.outputTokens(), cost);
    }

    @Transactional(readOnly = true)
    public PageResponse<GenerationResult> history(UUID workspaceId, UUID userId, int page, int size) {
        access.requireMember(workspaceId, userId);
        var result = generations
                .findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(page, size))
                .map(g -> new GenerationResult(g.getId(), g.getOutput(), g.getProvider(),
                        g.getModel(), g.getPromptTokens(), g.getOutputTokens(), g.getCreditsCost()));
        return PageResponse.from(result);
    }

    private int creditCost(AiLength length) {
        return switch (length) {
            case SHORT -> 1;
            case MEDIUM -> 2;
            case LONG -> 3;
        };
    }
}

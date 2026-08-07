package com.zyntral.modules.ai.web.dto;

import com.zyntral.modules.ai.domain.AiContentKind;
import com.zyntral.modules.ai.domain.AiLength;
import com.zyntral.modules.ai.domain.AiProviderKind;
import com.zyntral.modules.ai.domain.AiTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AiDtos {

    private AiDtos() {}

    public record GenerateRequest(
            @NotNull AiContentKind contentKind,
            AiTone tone,
            AiLength length,
            String language,
            @NotBlank @Size(max = 2000) String topic,
            @Size(max = 4000) String extraContext,
            AiProviderKind provider,   // optional — defaults to the configured provider
            String model               // optional — defaults to the provider's model
    ) {}

    public record GitHubGenerateRequest(
            @NotBlank @Size(max = 500) String repoUrl,
            @Size(max = 40) String commitSha,   // optional — defaults to latest commit
            String githubToken,                  // optional — for private repos
            AiTone tone,
            AiLength length,
            String language,
            AiProviderKind provider,
            String model
    ) {}

    public record GitHubFeatureRequest(
            @NotBlank @Size(max = 500) String repoUrl,
            @NotBlank @Size(max = 200) String featureName,
            @NotBlank @Size(max = 2000) String featureDescription,
            @Size(max = 2000) String filePaths,   // optional: comma-separated specific paths
            String githubToken,                    // optional: PAT for private repos
            AiContentKind contentKind,             // optional: defaults to LINKEDIN_POST
            AiTone tone,
            AiLength length,
            String language,
            AiProviderKind provider,
            String model
    ) {}

    public record CreditUsageResponse(int limit, int used, int remaining) {}
}

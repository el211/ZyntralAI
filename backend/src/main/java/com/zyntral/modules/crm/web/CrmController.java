package com.zyntral.modules.crm.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.crm.application.CrmProspectService;
import com.zyntral.modules.crm.domain.CrmProspect;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "CRM", description = "Prospect management and AI-powered outreach")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/crm")
public class CrmController {

    private final CrmProspectService service;

    public CrmController(CrmProspectService service) {
        this.service = service;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record ProspectRequest(
            @NotBlank @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 200) String company,
            @Size(max = 500) String website,
            @Size(max = 200) String email,
            @Size(max = 50)  String phone,
            @Size(max = 500) String linkedinUrl,
            @Size(max = 100) String country,
            @Size(max = 100) String industry,
            String notes
    ) {}

    public record UpdateProspectRequest(
            @NotBlank @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 200) String company,
            @Size(max = 500) String website,
            @Size(max = 200) String email,
            @Size(max = 50)  String phone,
            @Size(max = 500) String linkedinUrl,
            @Size(max = 100) String country,
            @Size(max = 100) String industry,
            String notes,
            @NotBlank String status
    ) {}

    public record AnalyzeWebsiteRequest(@NotBlank @Size(max = 500) String url) {}

    public record DraftMessageRequest(
            @NotNull UUID prospectId,
            @NotBlank String channel,       // EMAIL | LINKEDIN | SMS
            @Size(max = 500) String customNote
    ) {}

    public record ProspectResponse(
            UUID id,
            String firstName, String lastName,
            String company, String website,
            String email, String phone, String linkedinUrl,
            String country, String industry,
            String status, String notes,
            Instant lastContactedAt, Instant createdAt, Instant updatedAt
    ) {
        static ProspectResponse of(CrmProspect p) {
            return new ProspectResponse(
                    p.getId(), p.getFirstName(), p.getLastName(),
                    p.getCompany(), p.getWebsite(),
                    p.getEmail(), p.getPhone(), p.getLinkedinUrl(),
                    p.getCountry(), p.getIndustry(),
                    p.getStatus(), p.getNotes(),
                    p.getLastContactedAt(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record AiTextResponse(String text) {}

    public record DiscoverRequest(
            @NotBlank @Size(max = 100) String industry,
            @NotBlank @Size(max = 100) String country,
            @Size(max = 300) String keywords,
            int count   // 5, 10, or 20
    ) {}

    public record EnrichRequest(@NotBlank @Size(max = 500) String website) {}

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @Operation(summary = "Add a prospect")
    @PostMapping("/prospects")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProspectResponse> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ProspectRequest req) {
        CrmProspect p = service.create(workspaceId, SecurityUtils.currentUserId(),
                req.firstName(), req.lastName(), req.company(),
                req.website(), req.email(), req.phone(),
                req.linkedinUrl(), req.country(), req.industry(), req.notes());
        return ApiResponse.ok(ProspectResponse.of(p));
    }

    @Operation(summary = "List all prospects")
    @GetMapping("/prospects")
    public ApiResponse<List<ProspectResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(
                service.list(workspaceId, SecurityUtils.currentUserId())
                        .stream().map(ProspectResponse::of).toList());
    }

    @Operation(summary = "Update a prospect")
    @PutMapping("/prospects/{prospectId}")
    public ApiResponse<ProspectResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID prospectId,
            @Valid @RequestBody UpdateProspectRequest req) {
        CrmProspect p = service.update(workspaceId, SecurityUtils.currentUserId(), prospectId,
                req.firstName(), req.lastName(), req.company(),
                req.website(), req.email(), req.phone(),
                req.linkedinUrl(), req.country(), req.industry(),
                req.notes(), req.status());
        return ApiResponse.ok(ProspectResponse.of(p));
    }

    @Operation(summary = "Delete a prospect")
    @DeleteMapping("/prospects/{prospectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID prospectId) {
        service.delete(workspaceId, SecurityUtils.currentUserId(), prospectId);
    }

    @Operation(summary = "AI: Discover prospects matching criteria (charges 1 credit)")
    @PostMapping("/discover")
    public ApiResponse<List<CrmProspectService.DiscoveredProspect>> discover(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody DiscoverRequest req) {
        int count = req.count() > 0 ? Math.min(req.count(), 20) : 10;
        return ApiResponse.ok(service.discoverProspects(
                workspaceId, SecurityUtils.currentUserId(),
                req.industry(), req.country(), req.keywords(), count));
    }

    @Operation(summary = "Enrich a prospect by fetching their website for email + description (free)")
    @PostMapping("/enrich")
    public ApiResponse<CrmProspectService.EnrichmentResult> enrich(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody EnrichRequest req) {
        return ApiResponse.ok(service.enrichWebsite(
                workspaceId, SecurityUtils.currentUserId(), req.website()));
    }

    @Operation(summary = "AI: Analyze a website for outreach insights (charges 1 credit)")
    @PostMapping("/analyze-website")
    public ApiResponse<AiTextResponse> analyzeWebsite(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AnalyzeWebsiteRequest req) {
        String analysis = service.analyzeWebsite(workspaceId, SecurityUtils.currentUserId(), req.url());
        return ApiResponse.ok(new AiTextResponse(analysis));
    }

    @Operation(summary = "AI: Draft an outreach message for a prospect (charges 1 credit)")
    @PostMapping("/draft-message")
    public ApiResponse<AiTextResponse> draftMessage(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody DraftMessageRequest req) {
        String draft = service.draftMessage(workspaceId, SecurityUtils.currentUserId(),
                req.prospectId(), req.channel(), req.customNote());
        return ApiResponse.ok(new AiTextResponse(draft));
    }
}

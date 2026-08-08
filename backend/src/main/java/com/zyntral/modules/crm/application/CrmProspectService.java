package com.zyntral.modules.crm.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.application.AiCreditService;
import com.zyntral.modules.ai.application.AiProvider;
import com.zyntral.modules.ai.application.AiProviderRegistry;
import com.zyntral.modules.ai.domain.WorkspaceProviderKeyRepository;
import com.zyntral.modules.crm.domain.CrmProspect;
import com.zyntral.modules.crm.domain.CrmProspectRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * CRM prospect management and AI-powered outreach features.
 * Website analysis and message drafting consume credits (BYOK discount applies).
 */
@Service
public class CrmProspectService {

    private static final int CREDIT_SCALE = 2;

    private final CrmProspectRepository prospects;
    private final WorkspaceAccess access;
    private final AiProviderRegistry registry;
    private final AiCreditService credits;
    private final WorkspaceProviderKeyRepository providerKeys;
    private final RestClient http;

    public CrmProspectService(CrmProspectRepository prospects, WorkspaceAccess access,
                               AiProviderRegistry registry, AiCreditService credits,
                               WorkspaceProviderKeyRepository providerKeys) {
        this.prospects = prospects;
        this.access = access;
        this.registry = registry;
        this.credits = credits;
        this.providerKeys = providerKeys;
        this.http = RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ZyntralBot/1.0)")
                .build();
    }

    // ── Prospect CRUD ─────────────────────────────────────────────────────────

    @Transactional
    public CrmProspect create(UUID workspaceId, UUID userId,
                               String firstName, String lastName, String company,
                               String website, String email, String phone,
                               String linkedinUrl, String country, String industry, String notes) {
        access.requireCanEdit(workspaceId, userId);
        CrmProspect p = CrmProspect.create(workspaceId, userId,
                firstName, lastName, company, website, email, phone, linkedinUrl, country, industry, notes);
        return prospects.save(p);
    }

    @Transactional(readOnly = true)
    public List<CrmProspect> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return prospects.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional
    public CrmProspect update(UUID workspaceId, UUID userId, UUID prospectId,
                               String firstName, String lastName, String company,
                               String website, String email, String phone,
                               String linkedinUrl, String country, String industry,
                               String notes, String status) {
        access.requireCanEdit(workspaceId, userId);
        CrmProspect p = prospects.findByIdAndWorkspaceId(prospectId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("prospect", prospectId));
        p.update(firstName, lastName, company, website, email, phone,
                linkedinUrl, country, industry, notes, status);
        return prospects.save(p);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID userId, UUID prospectId) {
        access.requireCanEdit(workspaceId, userId);
        prospects.findByIdAndWorkspaceId(prospectId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("prospect", prospectId));
        prospects.deleteByIdAndWorkspaceId(prospectId, workspaceId);
    }

    // ── AI: Website Analysis ──────────────────────────────────────────────────

    public String analyzeWebsite(UUID workspaceId, UUID userId, String url) {
        access.requireCanEdit(workspaceId, userId);

        AiProvider provider = registry.resolve(null);
        String byokKey = providerKeys.find(workspaceId, provider.kind().name())
                .map(k -> k.getApiKey()).orElse(null);
        // 1 credit platform / 0.5 credit BYOK (in half-credit units)
        int cost = byokKey != null ? 1 : CREDIT_SCALE;
        credits.charge(workspaceId, cost);

        try {
            String html = fetchWebsite(url);
            String text = stripHtml(html);
            if (text.length() > 4000) text = text.substring(0, 4000) + "...";

            String system = "You are a website audit specialist and digital marketing expert. " +
                    "Analyze website content objectively and give actionable, specific insights.";

            String userPrompt = "Analyze this website content from " + url + " and provide:\n" +
                    "1. What products or services they offer\n" +
                    "2. Missing elements (CTA, social proof, contact info, pricing, etc.)\n" +
                    "3. Content quality and messaging gaps\n" +
                    "4. Opportunities to improve their online presence\n" +
                    "5. Key talking points for a sales outreach\n\n" +
                    "Website content:\n" + text + "\n\n" +
                    "Be concise and specific. Format as clear bullet points under each heading.";

            AiProvider.AiCompletion completion = provider.complete(new AiProvider.AiRequest(
                    system, userPrompt, null, 900, 0.5, byokKey));
            return completion.text();
        } catch (RuntimeException ex) {
            credits.refund(workspaceId, cost);
            throw ex;
        }
    }

    // ── AI: Message Drafting ──────────────────────────────────────────────────

    public String draftMessage(UUID workspaceId, UUID userId,
                                UUID prospectId, String channel, String customNote) {
        access.requireCanEdit(workspaceId, userId);

        CrmProspect prospect = prospects.findByIdAndWorkspaceId(prospectId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("prospect", prospectId));

        AiProvider provider = registry.resolve(null);
        String byokKey = providerKeys.find(workspaceId, provider.kind().name())
                .map(k -> k.getApiKey()).orElse(null);
        int cost = byokKey != null ? 1 : CREDIT_SCALE;
        credits.charge(workspaceId, cost);

        try {
            String channelInstructions = switch (channel.toUpperCase()) {
                case "EMAIL" -> "Write a professional cold outreach email. Include a compelling subject line on the first line " +
                        "formatted as 'Subject: ...' then a blank line then the body. Keep it under 150 words. " +
                        "Be direct, personal, and end with one clear CTA.";
                case "LINKEDIN" -> "Write two parts: first a LinkedIn connection request note (max 300 characters), " +
                        "then after '---' a follow-up DM to send after connecting (max 200 words). " +
                        "Both should be conversational, genuine, and reference their specific situation.";
                case "SMS"  -> "Write a short, casual SMS outreach message (max 160 characters). " +
                        "First introduce yourself briefly, then one clear reason you're reaching out, " +
                        "then a soft CTA. No emojis unless natural.";
                default     -> "Write a professional outreach message under 150 words.";
            };

            String prospectInfo = buildProspectContext(prospect);

            String system = "You are an expert B2B sales copywriter specializing in personalized, non-spammy outreach.";
            String userPrompt = "Draft a " + channel + " outreach message for the following prospect.\n\n" +
                    "Prospect details:\n" + prospectInfo + "\n\n" +
                    (customNote != null && !customNote.isBlank()
                            ? "Additional context from the sender: " + customNote + "\n\n"
                            : "") +
                    "Instructions: " + channelInstructions + "\n\n" +
                    "Make it personal and specific to their situation. Avoid generic templates.";

            AiProvider.AiCompletion completion = provider.complete(new AiProvider.AiRequest(
                    system, userPrompt, null, 700, 0.7, byokKey));

            prospect.markContacted();
            prospects.save(prospect);

            return completion.text();
        } catch (RuntimeException ex) {
            credits.refund(workspaceId, cost);
            throw ex;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildProspectContext(CrmProspect p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(p.getFirstName());
        if (p.getLastName() != null && !p.getLastName().isBlank())
            sb.append(" ").append(p.getLastName());
        sb.append("\n");
        if (p.getCompany() != null)   sb.append("Company: ").append(p.getCompany()).append("\n");
        if (p.getCountry() != null)   sb.append("Country: ").append(p.getCountry()).append("\n");
        if (p.getIndustry() != null)  sb.append("Industry: ").append(p.getIndustry()).append("\n");
        if (p.getWebsite() != null)   sb.append("Website: ").append(p.getWebsite()).append("\n");
        if (p.getEmail() != null)     sb.append("Email: ").append(p.getEmail()).append("\n");
        if (p.getNotes() != null && !p.getNotes().isBlank())
            sb.append("Notes: ").append(p.getNotes()).append("\n");
        return sb.toString();
    }

    private String fetchWebsite(String url) {
        try {
            return http.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"Could not fetch website: " + e.getMessage()});
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        String s = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        s = s.replaceAll("<[^>]+>", " ");
        s = s.replace("&amp;", "&").replace("&lt;", "<")
             .replace("&gt;", ">").replace("&nbsp;", " ").replace("&quot;", "\"");
        return s.replaceAll("\\s+", " ").trim();
    }
}

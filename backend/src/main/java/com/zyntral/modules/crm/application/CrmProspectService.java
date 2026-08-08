package com.zyntral.modules.crm.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final ObjectMapper json;
    private final RestClient http;

    public CrmProspectService(CrmProspectRepository prospects, WorkspaceAccess access,
                               AiProviderRegistry registry, AiCreditService credits,
                               WorkspaceProviderKeyRepository providerKeys,
                               ObjectMapper json) {
        this.prospects = prospects;
        this.access = access;
        this.registry = registry;
        this.credits = credits;
        this.providerKeys = providerKeys;
        this.json = json;
        this.http = RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ZyntralBot/1.0)")
                .build();
    }

    // ── Result records ────────────────────────────────────────────────────────

    public record DiscoveredProspect(
            String company, String website, String description,
            String country, String industry, String estimatedSize,
            String firstName, String lastName, String contactEmail
    ) {}

    public record EnrichmentResult(String email, String metaDescription) {}

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

    // ── AI: Prospect Discovery ────────────────────────────────────────────────

    /**
     * Uses AI to generate a list of real companies matching the given criteria.
     * Charges 1 credit (0.5 with BYOK). Results are based on AI training knowledge —
     * not real-time search — so they should be verified before outreach.
     */
    public List<DiscoveredProspect> discoverProspects(UUID workspaceId, UUID userId,
                                                       String industry, String country,
                                                       String keywords, int count) {
        access.requireCanEdit(workspaceId, userId);

        AiProvider provider = registry.resolve(null);
        String byokKey = providerKeys.find(workspaceId, provider.kind().name())
                .map(k -> k.getApiKey()).orElse(null);
        int cost = byokKey != null ? 1 : CREDIT_SCALE;
        credits.charge(workspaceId, cost);

        try {
            String system = "You are a B2B sales intelligence assistant with broad knowledge of companies " +
                    "across many countries and industries. You always respond with valid JSON only — " +
                    "no markdown, no commentary, just the JSON array.";

            int safeCount = Math.min(count, 10); // cap at 10 to avoid token truncation
            String userPrompt = "Find " + safeCount + " real companies matching these criteria:\n" +
                    "- Industry: " + industry + "\n" +
                    "- Country/Region: " + country + "\n" +
                    (keywords != null && !keywords.isBlank() ? "- Additional target: " + keywords + "\n" : "") +
                    "\nReturn a JSON array with exactly this structure — no markdown, just raw JSON:\n" +
                    "[{\n" +
                    "  \"company\": \"Exact company name\",\n" +
                    "  \"website\": \"https://their-domain.com\",\n" +
                    "  \"description\": \"One sentence: what they do and who they serve\",\n" +
                    "  \"country\": \"Country name\",\n" +
                    "  \"industry\": \"Industry category\",\n" +
                    "  \"estimatedSize\": \"small or medium or large\",\n" +
                    "  \"firstName\": \"Founder or CEO first name if you know it, otherwise null\",\n" +
                    "  \"lastName\": \"Founder or CEO last name if you know it, otherwise null\",\n" +
                    "  \"contactEmail\": \"public contact email if you know it, otherwise null\"\n" +
                    "}]\n\n" +
                    "Rules:\n" +
                    "- Only include companies you are confident are real and match the criteria\n" +
                    "- Always include https:// in website URLs\n" +
                    "- Prefer companies that likely need the services implied by the industry/keywords\n" +
                    "- Vary company sizes — include both well-known and smaller companies";

            // Each company object needs ~300-400 tokens; give generous headroom
            int maxTokens = Math.min(count, 10) * 500 + 500;
            AiProvider.AiCompletion completion = provider.complete(new AiProvider.AiRequest(
                    system, userPrompt, null, maxTokens, 0.4, byokKey));

            return parseDiscoveredProspects(completion.text());
        } catch (RuntimeException ex) {
            credits.refund(workspaceId, cost);
            throw ex;
        }
    }

    private List<DiscoveredProspect> parseDiscoveredProspects(String raw) {
        String text = raw == null ? "" : raw.trim();

        // Strip markdown code fences (```json ... ```)
        text = text.replaceAll("(?s)```[a-zA-Z]*\\s*", "").replace("```", "").trim();

        // Find the outermost JSON array
        int start = text.indexOf('[');
        if (start == -1) {
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"AI did not return a list of companies — please try again"});
        }

        // Walk from start to find the matching closing bracket, handling nesting
        int depth = 0;
        int end = -1;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { end = i; break; } }
        }

        if (end == -1) {
            // JSON was truncated — try to close it gracefully
            // Find last complete object (last "}")
            int lastBrace = text.lastIndexOf('}');
            if (lastBrace > start) {
                text = text.substring(start, lastBrace + 1) + "]";
            } else {
                throw new ApiException(ErrorCode.BUSINESS_RULE,
                        new Object[]{"AI response was incomplete — try a smaller count (5 or 10)"});
            }
        } else {
            text = text.substring(start, end + 1);
        }

        try {
            List<DiscoveredProspect> result = json.readValue(text,
                    new TypeReference<List<DiscoveredProspect>>() {});
            if (result == null || result.isEmpty()) {
                throw new ApiException(ErrorCode.BUSINESS_RULE,
                        new Object[]{"AI found no companies matching your criteria — try different keywords"});
            }
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"Could not parse AI response — please try again"});
        }
    }

    // ── Website Enrichment (no credit charge) ─────────────────────────────────

    /**
     * Fetches a website and extracts contact email + meta description.
     * No credits charged — pure web scraping.
     */
    public EnrichmentResult enrichWebsite(UUID workspaceId, UUID userId, String websiteUrl) {
        access.requireMember(workspaceId, userId);
        try {
            String html = fetchWebsiteQuiet(websiteUrl);
            String email = extractBestEmail(html);

            // Try /contact page if no email found on homepage
            if (email == null) {
                String base = websiteUrl.replaceAll("/$", "");
                for (String path : List.of("/contact", "/contact-us", "/about", "/about-us")) {
                    try {
                        String contactHtml = fetchWebsiteQuiet(base + path);
                        email = extractBestEmail(contactHtml);
                        if (email != null) break;
                    } catch (Exception ignored) {}
                }
            }

            String desc = extractMetaDescription(html);
            return new EnrichmentResult(email, desc);
        } catch (Exception e) {
            return new EnrichmentResult(null, null);
        }
    }

    private String extractBestEmail(String html) {
        if (html == null) return null;
        Set<String> found = new LinkedHashSet<>();

        // mailto: links are most reliable
        Matcher m = Pattern.compile("mailto:([^\"'>\\s?&#]+)", Pattern.CASE_INSENSITIVE).matcher(html);
        while (m.find()) found.add(m.group(1).toLowerCase());

        // Raw email patterns in text
        m = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,6}").matcher(html);
        while (m.find()) found.add(m.group().toLowerCase());

        return found.stream()
                .filter(e -> !e.contains("noreply") && !e.contains("no-reply")
                        && !e.contains("example.") && !e.contains("sentry.")
                        && !e.contains("wix.") && !e.contains("schema.org"))
                .findFirst()
                .orElse(found.stream().findFirst().orElse(null));
    }

    private String extractMetaDescription(String html) {
        if (html == null) return null;
        // <meta name="description" content="...">
        Matcher m = Pattern.compile(
                "<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']{10,300})[\"']",
                Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) return m.group(1);
        m = Pattern.compile(
                "<meta[^>]+content=[\"']([^\"']{10,300})[\"'][^>]+name=[\"']description[\"']",
                Pattern.CASE_INSENSITIVE).matcher(html);
        return m.find() ? m.group(1) : null;
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

    private String fetchWebsiteQuiet(String url) {
        try {
            return http.get().uri(url).retrieve().body(String.class);
        } catch (Exception e) {
            return null;
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

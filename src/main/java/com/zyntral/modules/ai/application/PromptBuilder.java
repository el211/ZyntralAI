package com.zyntral.modules.ai.application;

import com.zyntral.modules.ai.domain.AiContentKind;
import com.zyntral.modules.ai.domain.AiLength;
import com.zyntral.modules.ai.domain.AiTone;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Translates a structured generation request into a system + user prompt. Centralising
 * prompt construction keeps quality consistent and makes per-content-kind tuning a
 * one-file change. Custom workspace templates can override the user prompt upstream.
 */
@Component
public class PromptBuilder {

    public record Prompt(String system, String user) {}

    public Prompt build(AiContentKind kind, AiTone tone, AiLength length,
                        String language, String topic, String extraContext) {
        String system = systemPrompt(kind, tone, length, language);
        StringBuilder user = new StringBuilder();
        user.append(instruction(kind)).append("\n\nTopic / brief: ").append(topic);
        if (extraContext != null && !extraContext.isBlank()) {
            user.append("\n\nAdditional context: ").append(extraContext);
        }
        return new Prompt(system, user.toString());
    }

    private String systemPrompt(AiContentKind kind, AiTone tone, AiLength length, String language) {
        return """
               You are Zyntral AI, an expert social-media and marketing copywriter.
               Write %s content with a %s tone. Target length: %s.
               Respond in %s. Output only the finished copy — no preamble, no explanations,
               no surrounding quotation marks.""".formatted(
                humanize(kind.name()),
                tone == null ? "professional" : tone.name().toLowerCase(Locale.ROOT),
                lengthHint(length),
                languageName(language));
    }

    private String instruction(AiContentKind kind) {
        return switch (kind) {
            case LINKEDIN_POST -> "Write an engaging LinkedIn post. Use short paragraphs and a strong hook. Add 3-5 relevant hashtags at the end.";
            case X_POST -> "Write a concise, punchy post for X (Twitter) under 280 characters. Make the first line a hook.";
            case INSTAGRAM_CAPTION -> "Write an Instagram caption with an attention-grabbing first line, line breaks, relevant emojis, and 5-10 hashtags.";
            case TIKTOK_IDEA -> "Propose a TikTok video idea: a hook, a short shot-by-shot outline, an on-screen caption, and a trending-style CTA.";
            case FACEBOOK_POST -> "Write a Facebook post that drives engagement and invites comments.";
            case MARKETING_COPY -> "Write persuasive marketing copy that highlights benefits and ends with a clear call to action.";
            case PRODUCT_DESCRIPTION -> "Write a compelling product description: a benefit-led opening, key features as bullet points, and a closing line.";
            case EMAIL_CAMPAIGN -> "Write a marketing email: a subject line, a preview line, a body with one clear CTA, and a sign-off.";
            case BLOG_OUTLINE -> "Produce a structured blog article outline: a working title, an intro angle, H2/H3 section headings with one-line notes, and a conclusion.";
            case CTA -> "Write 5 distinct, high-converting calls to action as a numbered list.";
            case HASHTAGS -> "Suggest 15-20 relevant hashtags grouped from broad to niche, space-separated.";
        };
    }

    private String lengthHint(AiLength length) {
        if (length == null) return "medium";
        return switch (length) {
            case SHORT -> "short and tight";
            case MEDIUM -> "medium";
            case LONG -> "long and detailed";
        };
    }

    private String languageName(String code) {
        if (code == null) return "English";
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "fr" -> "French";
            default -> "English";
        };
    }

    private String humanize(String enumName) {
        return enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}

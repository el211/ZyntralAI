package com.zyntral.modules.support.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.support.application.SupportChatService;
import com.zyntral.modules.support.web.dto.SupportDtos.ChatRequest;
import com.zyntral.modules.support.web.dto.SupportDtos.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for the embeddable widget — no JWT. The agent is identified by its public key.
 * Mapped under {@code /support/public/**}, which the security chain permits anonymously.
 */
@Tag(name = "Support (public)", description = "Embeddable support chat widget endpoint")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/support/public")
public class SupportPublicController {

    private final SupportChatService chat;

    public SupportPublicController(SupportChatService chat) {
        this.chat = chat;
    }

    @Operation(summary = "Send a message to a support agent")
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        var result = chat.chat(req.publicKey(), req.visitorId(), req.conversationId(), req.message());
        return ApiResponse.ok(new ChatResponse(result.conversationId(), result.reply()));
    }
}

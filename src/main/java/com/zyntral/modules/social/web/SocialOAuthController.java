package com.zyntral.modules.social.web;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.social.application.SocialOAuthService;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Tag(name = "Social OAuth", description = "Connect social accounts via OAuth 2.0")
@RestController
@RequestMapping(ApiConstants.API_V1)
public class SocialOAuthController {

    private final SocialOAuthService oauth;
    private final SocialOAuthProperties props;

    public SocialOAuthController(SocialOAuthService oauth, SocialOAuthProperties props) {
        this.oauth = oauth;
        this.props = props;
    }

    public record ConnectUrlResponse(String authorizationUrl) {}

    @Operation(summary = "Start an OAuth connect flow; returns the provider consent URL (ADMIN+)")
    @GetMapping("/workspaces/{workspaceId}/social-accounts/connect/{platform}")
    public ApiResponse<ConnectUrlResponse> connect(@PathVariable UUID workspaceId,
                                                   @PathVariable String platform) {
        String url = oauth.startConnect(workspaceId, SecurityUtils.currentUserId(), parse(platform));
        return ApiResponse.ok(new ConnectUrlResponse(url));
    }

    /** Public callback hit by the provider's browser redirect — secured by the Redis state token. */
    @Operation(summary = "OAuth provider callback (redirects back to the app)")
    @GetMapping("/social/oauth/callback/{platform}")
    public ResponseEntity<Void> callback(@PathVariable String platform,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        if (error != null || code == null) {
            return redirect(props.webRedirectUrl() + "?error=" + (error == null ? "cancelled" : error));
        }
        String target = oauth.handleCallback(parse(platform), code, state);
        return redirect(target);
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    private SocialPlatform parse(String platform) {
        try {
            return SocialPlatform.valueOf(platform.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
    }
}

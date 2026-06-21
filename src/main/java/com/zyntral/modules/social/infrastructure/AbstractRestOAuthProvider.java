package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.social.application.SocialOAuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Shared HTTP plumbing for OAuth adapters: form-encoded token requests and bearer GETs.
 * Each concrete provider supplies only the URLs, params, scopes and profile mapping.
 */
public abstract class AbstractRestOAuthProvider implements SocialOAuthProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final RestClient http = RestClient.create();

    protected MultiValueMap<String, String> form(String... kv) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) form.add(kv[i], kv[i + 1]);
        }
        return form;
    }

    protected JsonNode postForm(String url, MultiValueMap<String, String> form, String basicAuth) {
        var req = http.post().uri(url).contentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (basicAuth != null) req = req.header("Authorization", "Basic " + basicAuth);
        return req.body(form).retrieve().body(JsonNode.class);
    }

    protected JsonNode getJson(String url, String bearer) {
        return http.get().uri(url).header("Authorization", "Bearer " + bearer)
                .retrieve().body(JsonNode.class);
    }

    protected RuntimeException connectFailed(String platform, Exception e) {
        log.error("{} OAuth exchange failed", platform, e);
        return new ApiException(ErrorCode.BUSINESS_RULE,
                new Object[]{"Could not connect " + platform + " account"});
    }

    protected String basic(String id, String secret) {
        return java.util.Base64.getEncoder()
                .encodeToString((id + ":" + secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

package com.zyntral.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyntral.common.error.ApiError;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Serializes the uniform {@link ApiError} envelope for security-chain failures (401/403). */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper mapper;
    private final MessageService messages;

    public SecurityErrorWriter(ObjectMapper mapper, MessageService messages) {
        this.mapper = mapper;
        this.messages = messages;
    }

    public void write(HttpServletRequest req, HttpServletResponse res, ErrorCode code)
            throws IOException {
        res.setStatus(code.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = ApiError.of(code.status().value(), code,
                messages.get(code.messageKey()), req.getRequestURI(), MDC.get("traceId"), null);
        mapper.writeValue(res.getOutputStream(), body);
    }
}

package com.zyntral.common.error;

import com.zyntral.common.i18n.MessageService;
import com.zyntral.modules.dubbing.infrastructure.DubbingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centralised translation of exceptions into the uniform {@link ApiError} envelope.
 * Every code path that leaves a controller abnormally lands here, so clients always
 * see the same shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageService messages;

    public GlobalExceptionHandler(MessageService messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        ErrorCode code = ex.code();
        return build(code, messages.get(code.messageKey(), ex.args()), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(ErrorCode.VALIDATION_FAILED,
                messages.get(ErrorCode.VALIDATION_FAILED.messageKey()), req, fields);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(ErrorCode.UNAUTHORIZED,
                messages.get(ErrorCode.UNAUTHORIZED.messageKey()), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(ErrorCode.FORBIDDEN,
                messages.get(ErrorCode.FORBIDDEN.messageKey()), req, null);
    }

    @ExceptionHandler(DubbingException.class)
    public ResponseEntity<ApiError> handleDubbing(DubbingException ex, HttpServletRequest req) {
        log.warn("Dubbing error on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(ErrorCode.BUSINESS_RULE, ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL,
                messages.get(ErrorCode.INTERNAL.messageKey()), req, null);
    }

    private ApiError.FieldError toFieldError(FieldError fe) {
        return new ApiError.FieldError(fe.getField(),
                fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest req,
                                           List<ApiError.FieldError> fields) {
        HttpStatus status = code.status();
        ApiError body = ApiError.of(status.value(), code, message, req.getRequestURI(),
                MDC.get("traceId"), fields);
        return ResponseEntity.status(status).body(body);
    }
}

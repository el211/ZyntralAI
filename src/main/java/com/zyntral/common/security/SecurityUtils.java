package com.zyntral.common.security;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Convenience access to the authenticated {@link AppPrincipal} from anywhere. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<AppPrincipal> currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    /** The current user id, or throws 401 if unauthenticated. */
    public static UUID currentUserId() {
        return currentPrincipal()
                .map(AppPrincipal::getUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}

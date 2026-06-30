package com.zyntral.modules.dubbing.infrastructure;

/** Raised when a call to the ElevenLabs Dubbing API fails. */
public class DubbingException extends RuntimeException {

    public DubbingException(String message) {
        super(message);
    }

    public DubbingException(String message, Throwable cause) {
        super(message, cause);
    }
}

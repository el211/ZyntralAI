package com.zyntral.modules.dubbing.domain;

/** Lifecycle of an ElevenLabs dubbing job. Mirrors PostgreSQL {@code dubbing_status}. */
public enum DubbingStatus {
    QUEUED, DUBBING, DUBBED, FAILED
}

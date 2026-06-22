package com.zyntral.modules.ai.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Advances in-flight Veo video jobs on a fixed interval. */
@Component
public class VideoPoller {

    private final VideoGenerationService videos;

    public VideoPoller(VideoGenerationService videos) {
        this.videos = videos;
    }

    @Scheduled(fixedDelay = 20_000, initialDelay = 20_000)
    public void poll() {
        videos.pollPending();
    }
}


package com.zyntral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Zyntral AI — application entry point.
 *
 * <p>Modular monolith: each bounded context lives under {@code com.zyntral.modules.*}
 * with its own web / application / domain / infrastructure layers. Shared kernel
 * (error model, security, i18n, pagination) lives under {@code com.zyntral.common}.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class ZyntralApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZyntralApplication.class, args);
    }
}

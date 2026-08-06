package com.zyntral.common.config;

import com.zyntral.common.crypto.CryptoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activates @ConfigurationProperties beans that aren't enabled alongside their own config. */
@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class PropertiesConfig {
}

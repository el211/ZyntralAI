package com.zyntral.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the request locale from the {@code Accept-Language} header. Supported
 * languages are English and French; anything else falls back to English. Adding a
 * language = drop a {@code messages_xx.properties} and list it here.
 */
@Configuration
public class WebI18nConfig {

    private static final Locale DEFAULT = Locale.ENGLISH;
    private static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.FRENCH);

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(DEFAULT);
        resolver.setSupportedLocales(SUPPORTED);
        return resolver;
    }
}

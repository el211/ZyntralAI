package com.zyntral.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Thin wrapper over Spring's {@link MessageSource} that resolves against the locale
 * carried on the current request (set from the {@code Accept-Language} header). Backend
 * translations live in {@code resources/i18n/messages_{lang}.properties}.
 */
@Service
public class MessageService {

    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key, Object... args) {
        return get(key, LocaleContextHolder.getLocale(), args);
    }

    public String get(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}

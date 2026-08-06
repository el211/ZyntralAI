package com.zyntral.common.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringCryptoConverterTest {

    private final StringCryptoConverter converter =
            new StringCryptoConverter(new CryptoProperties("unit-test-crypto-secret-passphrase"));

    @Test
    void encryptsAndDecryptsRoundTrip() {
        String plaintext = "ya29.super-secret-oauth-token";

        String encrypted = converter.convertToDatabaseColumn(plaintext);

        assertThat(encrypted).isNotNull().isNotEqualTo(plaintext);
        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void producesDifferentCiphertextEachTime() {
        String a = converter.convertToDatabaseColumn("same-value");
        String b = converter.convertToDatabaseColumn("same-value");

        assertThat(a).isNotEqualTo(b);   // random IV per encryption
        assertThat(converter.convertToEntityAttribute(a)).isEqualTo("same-value");
        assertThat(converter.convertToEntityAttribute(b)).isEqualTo("same-value");
    }

    @Test
    void passesNullThrough() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

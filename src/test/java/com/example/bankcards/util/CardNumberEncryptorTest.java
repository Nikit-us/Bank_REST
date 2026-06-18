package com.example.bankcards.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.example.bankcards.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardNumberEncryptorTest {

    private static final String PASSWORD = "test-encryption-password";
    private static final String SALT = "58fccc618bb9c69c";
    private static final String HMAC_KEY = "aaIWNTXqFOQfOmtviW/qSQHjRbc4SBb2g4//wyKoBxw=";

    private CardNumberEncryptor encryptor;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getEncryption().setPassword(PASSWORD);
        props.getEncryption().setSalt(SALT);
        props.getEncryption().setHmacKey(HMAC_KEY);
        encryptor = new CardNumberEncryptor(props);
    }

    @Test
    void encryptThenDecryptReturnsOriginal() {
        String card = "4111111111111111";

        String encrypted = encryptor.encrypt(card);

        assertAll(
                () -> assertThat(encrypted).isNotEqualTo(card),
                () -> assertThat(encryptor.decrypt(encrypted)).isEqualTo(card)
        );
    }

    @Test
    void encryptionIsNonDeterministicButHashIsDeterministic() {
        String card = "4111111111111111";

        assertAll(
                () -> assertThat(encryptor.encrypt(card)).isNotEqualTo(encryptor.encrypt(card)),
                () -> assertThat(encryptor.hash(card)).isEqualTo(encryptor.hash(card)),
                () -> assertThat(encryptor.hash(card)).isNotEqualTo(encryptor.hash("5111111111111111"))
        );
    }

    @Test
    void maskUsesLastFourDigits() {
        assertAll(
                () -> assertThat(CardMaskUtil.mask("1234")).isEqualTo("**** **** **** 1234"),
                () -> assertThat(CardMaskUtil.lastFour("4111111111111111")).isEqualTo("1111")
        );
    }
}

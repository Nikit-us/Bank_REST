package com.example.bankcards.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
@Getter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Encryption encryption = new Encryption();

    @Getter
    @Setter
    public static class Jwt {

        @NotBlank
        private String secret;

        @Positive
        private long expirationMs = 3_600_000L;

        private String issuer = "bank-rest";
    }

    @Getter
    @Setter
    public static class Encryption {

        @NotBlank
        private String password;

        @NotBlank
        private String salt;

        @NotBlank
        private String hmacKey;
    }
}

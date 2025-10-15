package com.scheduler;

import com.scheduler.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    // Valid Base64-encoded 256-bit key
    private static final String SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dFRlc3RpbmdPbmx5MTIz";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3_600_000L);
    }

    @Test
    void generateToken_thenValidate_succeeds() {
        String token = provider.generateToken("alice");
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void getUsernameFromToken_returnsCorrectSubject() {
        String token = provider.generateToken("alice");
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
    }

    @Test
    void validateToken_tamperedSignature_returnsFalse() {
        String token  = provider.generateToken("alice");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws Exception {
        // Create a provider with 1 ms expiry
        JwtTokenProvider shortLived = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLived, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(shortLived, "jwtExpirationMs", 1L);

        String token = shortLived.generateToken("alice");
        Thread.sleep(10); // let it expire

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_garbage_returnsFalse() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
    }
}

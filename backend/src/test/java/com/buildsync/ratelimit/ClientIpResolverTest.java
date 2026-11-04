package com.buildsync.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    @DisplayName("Scenario 9: RemoteAddr used and X-Forwarded-For ignored when trusted proxy is false")
    void testUntrustedProxyIgnoresForwardedHeader() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxyEnabled(false);

        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.199");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Scenario 10: Client IP extracted from X-Forwarded-For when trusted proxy is true")
    void testTrustedProxyExtractsClientIp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxyEnabled(true);

        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.45, 198.51.100.1, 10.0.0.2");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("203.0.113.45");
    }

    @Test
    @DisplayName("Scenario 11: X-Real-IP fallback when trusted proxy is true and X-Forwarded-For is missing")
    void testTrustedProxyXRealIpFallback() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxyEnabled(true);

        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.3");
        request.addHeader("X-Real-IP", "198.51.100.99");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("198.51.100.99");
    }

    @Test
    @DisplayName("Scenario 12: Blank or unknown header values fall back safely to remote address")
    void testBlankHeaderFallbackToRemoteAddr() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxyEnabled(true);

        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.5");
        request.addHeader("X-Forwarded-For", "unknown,  ");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("172.16.0.5");
    }
}


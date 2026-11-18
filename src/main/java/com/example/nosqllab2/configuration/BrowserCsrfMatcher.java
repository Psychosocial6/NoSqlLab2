package com.example.nosqllab2.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.net.URI;

final class BrowserCsrfMatcher implements RequestMatcher {
    @Override
    public boolean matches(HttpServletRequest request) {
        return CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request) && !isSameOrigin(request);
    }

    private static boolean isSameOrigin(HttpServletRequest request) {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (fetchSite != null) {
            return "same-origin".equals(fetchSite);
        }
        String source = request.getHeader(HttpHeaders.ORIGIN);
        if (source == null) {
            source = request.getHeader(HttpHeaders.REFERER);
        }
        if (source == null) {
            return false;
        }
        try {
            URI origin = URI.create(source);
            String scheme = origin.getScheme();
            if (origin.getHost() == null || origin.getUserInfo() != null || scheme == null) {
                return false;
            }
            int port = origin.getPort() >= 0 ? origin.getPort() : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
            return scheme.equalsIgnoreCase(request.getScheme())
                    && origin.getHost().equalsIgnoreCase(request.getServerName())
                    && port == request.getServerPort();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

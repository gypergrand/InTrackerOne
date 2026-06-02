package com.intrackerone.infrastructure.security;

import com.intrackerone.domain.user.ApiToken;
import com.intrackerone.domain.user.ApiTokenRepository;
import com.intrackerone.domain.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_ACCESS = "API_ACCESS";
    private static final String TOKEN_PREFIX = "itr_";
    private static final int MIN_TOKEN_LENGTH = TOKEN_PREFIX.length() + 16; // prefix + at least 16 chars of identifier payload

    private final ApiTokenRepository apiTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            if (authenticate(authHeader.substring(BEARER_PREFIX.length()), response)) {
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    /**
     * @return true if the request should be stopped (response already committed), false to continue the chain
     */
    private boolean authenticate(String rawToken, HttpServletResponse response) throws IOException {
        if (rawToken == null || rawToken.length() < MIN_TOKEN_LENGTH || !rawToken.startsWith(TOKEN_PREFIX)) {
            return false;
        }
        int identifierEnd = Math.min(rawToken.length(), TOKEN_PREFIX.length() + 16);
        String identifier = rawToken.substring(TOKEN_PREFIX.length(), identifierEnd);
        ApiToken token = apiTokenRepository.findByTokenIdentifier(identifier).orElse(null);
        if (token == null) {
            return false;
        }
        if (Boolean.FALSE.equals(token.getEnabled())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token has been revoked");
            return true;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String hashHex = HexFormat.of().formatHex(hash);
            if (MessageDigest.isEqual(hashHex.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    token.getTokenHash().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                setAuthentication(token);
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available
        }
        return false;
    }

    private void setAuthentication(ApiToken token) {
        User user = token.getUser();
        if (user.getEnabled() != null && !user.getEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (token.getLastUsedAt() == null || ChronoUnit.MINUTES.between(token.getLastUsedAt(), now) >= 5) {
            token.setLastUsedAt(now);
            apiTokenRepository.save(token);
        }

        var auth = new UsernamePasswordAuthenticationToken(
            user.getUsername(), null, authoritiesFor(user)
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Collection<? extends SimpleGrantedAuthority> authoritiesFor(User user) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        authorities.add(new SimpleGrantedAuthority(API_ACCESS));
        return authorities;
    }
}

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
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

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
            authenticate(authHeader.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    private void authenticate(String rawToken) {
        if (rawToken == null || rawToken.length() < MIN_TOKEN_LENGTH || !rawToken.startsWith(TOKEN_PREFIX)) {
            return;
        }
        // Extract the token identifier (prefix) used for DB lookup — matches how
        // ApiTokenService.create() stores identifier = rawToken.substring(4, 20)
        int identifierEnd = Math.min(rawToken.length(), TOKEN_PREFIX.length() + 16);
        String identifier = rawToken.substring(TOKEN_PREFIX.length(), identifierEnd);
        ApiToken token = apiTokenRepository.findByTokenIdentifier(identifier).orElse(null);
        if (token == null || Boolean.FALSE.equals(token.getEnabled())) {
            return;
        }
        if (BCrypt.checkpw(rawToken, token.getTokenHash())) {
            setAuthentication(token);
        }
    }

    private void setAuthentication(ApiToken token) {
        User user = token.getUser();
        if (user.getEnabled() != null && !user.getEnabled()) {
            return;
        }
        token.setLastUsedAt(LocalDateTime.now());
        apiTokenRepository.save(token);

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

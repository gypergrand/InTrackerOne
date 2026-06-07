package com.intrackerone.infrastructure.config;

import com.intrackerone.infrastructure.security.TokenAuthenticationFilter;
import com.intrackerone.ui.views.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 5)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PathPatternRequestMatcher.pathPattern("/api/**")).hasAuthority("API_ACCESS")
        );

        http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                        PathPatternRequestMatcher.pathPattern("/api/**")
                )
                .defaultAccessDeniedHandlerFor(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_FORBIDDEN),
                        PathPatternRequestMatcher.pathPattern("/api/**")
                )
        );

        http.csrf(csrf -> csrf
                .ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/api/**"))
                .ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/login"))
        );
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.with(VaadinSecurityConfigurer.vaadin(), cfg -> cfg.loginView(LoginView.class).defaultSuccessUrl("/", true));

        return http.build();
    }

    /**
     * Dev-only security chain: permits H2 console access for local development.
     * The securityMatcher ensures this chain only applies to /h2-console/**
     * and takes precedence over any other chain for this path.
     */
    @Configuration
    @Profile("dev")
    static class DevSecurityConfig {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain devH2SecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher(PathPatternRequestMatcher.pathPattern("/h2-console/**"))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable())
                    .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
            return http.build();
        }
    }

    /**
     * Non-dev safety net: explicitly denies H2 console access in production.
     * Without this, unmatched paths fall through to the default Vaadin chain
     * which may not have explicit rules for /h2-console.
     */
    @Configuration
    @Profile("!dev")
    static class ProdSecurityConfig {

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE - 10)
        SecurityFilterChain h2DenyAllChain(HttpSecurity http) throws Exception {
            http.securityMatcher(PathPatternRequestMatcher.pathPattern("/h2-console/**"))
                    .authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
            return http.build();
        }
    }
}

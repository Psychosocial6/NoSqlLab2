package com.example.nosqllab2.configuration;

import com.example.nosqllab2.users.CustomUserDetailsService;
import com.example.nosqllab2.users.PasswordHashes;
import com.example.nosqllab2.users.UserRoles;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contexts,
                                    CsrfTokenRepository csrfTokens, SessionRegistry sessionRegistry) throws Exception {
        return http
                .securityContext(context -> context.securityContextRepository(contexts))
                .addFilterAt(new ConcurrentSessionFilter(sessionRegistry,
                        event -> event.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED)),
                        ConcurrentSessionFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .requireCsrfProtectionMatcher(new BrowserCsrfMatcher()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico",
                                "/api/auth/csrf", "/api/product", "/api/product/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/user",
                                "/api/resetData/createToken", "/api/resetData/reset").permitAll()
                        .requestMatchers("/api/product", "/api/product/**", "/api/user", "/api/user/**",
                                "/api/migration/**", "/api/operations/speedtest/**").hasRole(UserRoles.ADMIN)
                        .requestMatchers("/api/favorite", "/api/favorite/**", "/api/operations/**", "/api/lead")
                                .hasAnyRole(UserRoles.TEACHER, UserRoles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/analytics/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/admin/notifications/deliveries").hasRole(UserRoles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/notifications", "/api/notifications/*/attempts",
                                "/ws/notifications").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK)))
                .build();
    }

    @Bean
    SecurityContextRepository securityContexts() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokens() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokens, SessionRegistry sessionRegistry) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokens),
                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher sessionEvents() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordHashes.encoder();
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsPasswordService(users);
        return new ProviderManager(provider);
    }
}

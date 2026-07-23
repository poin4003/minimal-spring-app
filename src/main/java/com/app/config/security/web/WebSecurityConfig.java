package com.app.config.security.web;

import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.app.config.settings.AppProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain webSecurityFilterChain(
            HttpSecurity http,
            WebJwtAuthenticationFilter webJwtAuthenticationFilter,
            WebSessionRefreshFilter webSessionRefreshFilter,
            WebAuthenticationEntryPoint webAuthenticationEntryPoint) throws Exception {
        String[] publicPaths = appProperties.getSecurity()
                .getWebPublicPaths()
                .toArray(String[]::new);
        String[] csrfIgnorePaths = Stream.concat(
                appProperties.getSecurity().getCsrfIgnorePaths().stream(),
                Stream.of(appProperties.getUi().getLogoutPath()))
                .distinct()
                .toArray(String[]::new);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CookieCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(csrfIgnorePaths))
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(webAuthenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicPaths).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(webSessionRefreshFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(webJwtAuthenticationFilter, WebSessionRefreshFilter.class);

        return http.build();
    }
}

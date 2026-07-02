package com.travel.user.config;

import com.travel.user.security.InternalApiKeyFilter;
import com.travel.user.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, InternalApiKeyFilter internalApiKeyFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.internalApiKeyFilter = internalApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/legal/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/internal").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                    .contentSecurityPolicy("default-src 'self'")
                    .and()
                    .frameOptions(frame -> frame.deny())
                    .and()
                    .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                    )
                );
        return http.build();
    }
}

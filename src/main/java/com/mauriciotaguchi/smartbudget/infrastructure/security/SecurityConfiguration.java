package com.mauriciotaguchi.smartbudget.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    org.springframework.security.core.userdetails.UserDetailsService disabledPasswordAuthentication() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use a credencial individual Bearer.");
        };
    }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http, AuthPrincipalRepository principals,
                                           OpaqueTokenService tokens) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(access -> access
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setHeader("Cache-Control", "no-store");
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Autenticação necessária.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Acesso negado.\"}");
                        }))
                .addFilterBefore(new BearerTokenFilter(principals, tokens), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}

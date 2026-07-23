package com.llenroctech.customerconnect.config;

import com.llenroctech.customerconnect.security.filter.JwtAuthorizationFilter;
import com.llenroctech.customerconnect.security.handler.ApiAccessDeniedHandler;
import com.llenroctech.customerconnect.security.handler.ApiAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/user/register",
            "/user/login",
            "/user/verify-code",
            "/user/refresh-token"
    };

    private final BCryptPasswordEncoder encoder;
    private final UserDetailsService userDetailsService;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_URLS).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/user/reset-password/*",
                                "/user/verify/password/*",
                                "/user/verify/account/*"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/user/reset-password"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/user/delete/**"
                        ).hasAuthority("DELETE:USER")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/customer/delete/**"
                        ).hasAuthority("DELETE:CUSTOMER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(
                        jwtAuthorizationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:4201"
        ));

        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.ORIGIN,
                "X-Requested-With"
        ));

        configuration.setExposedHeaders(List.of(
                HttpHeaders.AUTHORIZATION
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(encoder);

        return new ProviderManager(provider);
    }
}
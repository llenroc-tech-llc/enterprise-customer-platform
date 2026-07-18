package com.llenroctech.customerconnect.config;

import com.llenroctech.customerconnect.security.handler.ApiAccessDeniedHandler;
import com.llenroctech.customerconnect.security.handler.ApiAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final BCryptPasswordEncoder encoder;
    private final UserDetailsService userDetailsService;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    private static final String[] PUBLIC_URLS = {
            "/user/register",
            "/user/login",
            "/user/verify-code",
            "/user/refresh-token"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_URLS).permitAll()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/user/delete/**"
                        )
                        .hasAuthority("DELETE:USER")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/customer/delete/**"
                        )
                        .hasAuthority("DELETE:CUSTOMER")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )

                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(encoder);

        return new ProviderManager(provider);
    }
}

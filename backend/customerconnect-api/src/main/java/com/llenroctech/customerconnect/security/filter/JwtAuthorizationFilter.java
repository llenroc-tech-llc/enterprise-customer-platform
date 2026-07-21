package com.llenroctech.customerconnect.security.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.llenroctech.customerconnect.provider.TokenProvider;
import com.llenroctech.customerconnect.security.handler.ApiAuthenticationEntryPoint;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
            reject(request, response, null);
            return;
        }

        try {
            String subject = tokenProvider.verifyAccessToken(token).getSubject();
            if (subject == null || subject.isBlank()) {
                throw new BadCredentialsException("Access token subject is invalid");
            }

            CustomerConnectUserPrincipal principal = requirePrincipal(
                    userDetailsService.loadUserByUsername(subject)
            );
            validateAccountStatus(principal);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JWTVerificationException
                 | UsernameNotFoundException
                 | IllegalArgumentException exception) {
            reject(request, response, exception);
            return;
        } catch (BadCredentialsException exception) {
            reject(request, response, exception);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private CustomerConnectUserPrincipal requirePrincipal(UserDetails userDetails) {
        if (userDetails instanceof CustomerConnectUserPrincipal principal) {
            return principal;
        }
        throw new BadCredentialsException("Authenticated user is invalid");
    }

    private void validateAccountStatus(UserDetails userDetails) {
        if (!userDetails.isEnabled()
                || !userDetails.isAccountNonLocked()
                || !userDetails.isAccountNonExpired()
                || !userDetails.isCredentialsNonExpired()) {
            throw new BadCredentialsException("User account is unavailable");
        }
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception cause
    ) throws IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Access token is invalid", cause)
        );
    }
}

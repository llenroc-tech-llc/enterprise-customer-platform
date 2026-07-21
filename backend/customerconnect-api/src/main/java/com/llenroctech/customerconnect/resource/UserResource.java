package com.llenroctech.customerconnect.resource;

import com.llenroctech.customerconnect.domain.HttpResponse;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.config.JwtProperties;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.provider.TokenProvider;
import com.llenroctech.customerconnect.request.LoginRequest;
import com.llenroctech.customerconnect.request.MfaVerificationRequest;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import com.llenroctech.customerconnect.service.UserService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static java.time.LocalDateTime.now;
import static java.util.Map.of;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserResource {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final Environment environment;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/user/refresh-token";

    @PostMapping("/login")
    public ResponseEntity<HttpResponse> login(
            @RequestBody @Valid LoginRequest loginRequest,
            HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserDTO userDTO =
                userService.getUserByEmail(loginRequest.getEmail());

        return userDTO.isUsingMfa()
                ? sendVerificationCode(userDTO)
                : sendLoginResponse(
                        userDTO,
                        requirePrincipal(authentication.getPrincipal()),
                        response
                );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<HttpResponse> refreshToken(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            ) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        try {
            String email = tokenProvider.verifyRefreshToken(refreshToken)
                    .getSubject();
            CustomerConnectUserPrincipal principal = requirePrincipal(
                    userDetailsService.loadUserByUsername(email)
            );
            validateAccountStatus(principal);

            String accessToken = tokenProvider.createAccessToken(principal);
            addRefreshTokenCookie(
                    response,
                    tokenProvider.createRefreshToken(principal)
            );

            return ResponseEntity.ok(
                    HttpResponse.builder()
                            .timestamp(now().toString())
                            .data(of("accessToken", accessToken))
                            .message("Token refreshed")
                            .status(OK)
                            .statusCode(OK.value())
                            .build()
            );
        } catch (JWTVerificationException exception) {
            throw new BadCredentialsException(
                    "Refresh token is invalid",
                    exception
            );
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<HttpResponse> verifyMfaCode(
            @RequestBody @Valid MfaVerificationRequest request,
            HttpServletResponse response
    ) {
        if (!userService.verifyCode(
                request.getEmail(),
                request.getCode()
        )) {
            throw new BadCredentialsException("Verification code is invalid");
        }

        CustomerConnectUserPrincipal principal = requirePrincipal(
                userDetailsService.loadUserByUsername(request.getEmail())
        );
        validateAccountStatus(principal);

        return sendLoginResponse(
                userService.getUserByEmail(request.getEmail()),
                principal,
                response
        );
    }

    @PostMapping("/register")
    public ResponseEntity<HttpResponse> saveUser(
            @RequestBody @Valid User user) {

        UserDTO userDTO = userService.createUser(user);

        return ResponseEntity
                .created(getUserUri(userDTO.getId()))
                .body(
                        HttpResponse.builder()
                                .timestamp(now().toString())
                                .data(of("user", userDTO))
                                .message("User created")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build()
                );
    }

    @GetMapping("/profile")
    public ResponseEntity<HttpResponse> getProfile(Authentication authentication) {
        CustomerConnectUserPrincipal principal = requirePrincipal(
                authentication.getPrincipal()
        );

        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timestamp(now().toString())
                        .data(of(
                                "user",
                                userService.getUserByEmail(principal.getUsername())
                        ))
                        .message("User profile retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    private ResponseEntity<HttpResponse> sendLoginResponse(
            UserDTO userDTO,
            CustomerConnectUserPrincipal principal,
            HttpServletResponse response) {

        String accessToken = tokenProvider.createAccessToken(principal);
        addRefreshTokenCookie(
                response,
                tokenProvider.createRefreshToken(principal)
        );

        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timestamp(now().toString())
                        .data(of(
                                "user", userDTO,
                                "accessToken", accessToken
                        ))
                        .message("Login successful")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    private ResponseEntity<HttpResponse> sendVerificationCode(
            UserDTO userDTO) {

        userService.sendVerificationCode(userDTO);

        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timestamp(now().toString())
                        .data(of("user", userDTO))
                        .message("Verification code sent")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    private URI getUserUri(Long userId) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/user/get/{userId}")
                .buildAndExpand(userId)
                .toUri();
    }

    private CustomerConnectUserPrincipal requirePrincipal(Object principal) {
        if (principal instanceof CustomerConnectUserPrincipal userPrincipal) {
            return userPrincipal;
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

    private void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken
    ) {
        boolean localDevelopment =
                environment.acceptsProfiles(Profiles.of("dev"))
                        && !environment.acceptsProfiles(Profiles.of("prod"));

        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(!localDevelopment)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofMillis(
                        jwtProperties.refreshTokenExpirationMs()
                ))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

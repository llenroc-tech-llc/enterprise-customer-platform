package com.llenroctech.customerconnect.resource;

import com.llenroctech.customerconnect.domain.HttpResponse;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.request.LoginRequest;
import com.llenroctech.customerconnect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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

    @PostMapping("/login")
    public ResponseEntity<HttpResponse> login(
            @RequestBody @Valid LoginRequest loginRequest) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserDTO userDTO =
                userService.getUserByEmail(loginRequest.getEmail());

        return userDTO.isUsingMfa()
                ? sendVerificationCode(userDTO)
                : sendLoginResponse(userDTO);
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

    private ResponseEntity<HttpResponse> sendLoginResponse(
            UserDTO userDTO) {

        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timestamp(now().toString())
                        .data(of("user", userDTO))
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
}
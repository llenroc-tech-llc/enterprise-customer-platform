package com.llenroctech.customerconnect.service.implementation;

import com.llenroctech.customerconnect.domain.PasswordResetVerification;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.exception.ExpiredPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.InvalidPasswordResetTokenException;
import com.llenroctech.customerconnect.exception.InvalidAccountVerificationException;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.request.PasswordResetRequest;
import com.llenroctech.customerconnect.service.SmsService;
import com.llenroctech.customerconnect.provider.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private static final String VERIFICATION_CODE = "48392157";
    private static final String RESET_TOKEN =
            "20c42df8-16b4-4b2f-b01a-9635594ab85c";

    @Test
    void sendsGeneratedVerificationCode() {
        UserRepository<User> repository = repository();
        UserDTO user = userWithPhone("+1 (555) 555-5262");
        when(repository.createVerificationCode(user)).thenReturn(VERIFICATION_CODE);
        SmsService smsService = mock(SmsService.class);

        service(repository, smsService, mock(BCryptPasswordEncoder.class))
                .sendVerificationCode(user);

        verify(smsService).sendVerificationCode(
                user.getPhone(),
                VERIFICATION_CODE
        );
    }

    @Test
    void verifiesCodeThroughRepository() {
        UserRepository<User> repository = repository();
        when(repository.verifyCode("user@example.com", VERIFICATION_CODE))
                .thenReturn(true);

        boolean verified = service(
                repository,
                mock(SmsService.class),
                mock(BCryptPasswordEncoder.class)
        ).verifyCode("user@example.com", VERIFICATION_CODE);

        assertThat(verified).isTrue();
    }

    @Test
    void verifiesAccountAndReportsRepositoryState() {
        UserRepository<User> repository = repository();
        when(repository.verifyAccount("verification-key"))
                .thenReturn(false, true);
        UserServiceImpl service = service(repository);

        assertThat(service.verifyAccount(" verification-key ")
                .alreadyVerified()).isFalse();
        assertThat(service.verifyAccount("verification-key")
                .alreadyVerified()).isTrue();
        verify(repository, org.mockito.Mockito.times(2))
                .verifyAccount("verification-key");
    }

    @Test
    void rejectsBlankAccountVerificationKey() {
        UserRepository<User> repository = repository();

        assertThatThrownBy(() -> service(repository).verifyAccount(" "))
                .isInstanceOf(InvalidAccountVerificationException.class);
        verify(repository, never()).verifyAccount(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void validRefreshTokenCreatesRotatedTokenPair() {
        UserRepository<User> repository = repository();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        CustomerConnectUserPrincipal principal = principal(true, true);
        when(tokenProvider.verifyRefreshToken("refresh-token"))
                .thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com"))
                .thenReturn(principal);
        when(tokenProvider.createAccessToken(principal))
                .thenReturn("new-access-token");
        when(tokenProvider.createRefreshToken(principal))
                .thenReturn("new-refresh-token");

        var result = service(
                repository,
                mock(SmsService.class),
                mock(BCryptPasswordEncoder.class),
                tokenProvider,
                userDetailsService
        ).refreshAccessToken("refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void malformedRefreshTokenIsMappedToAuthenticationFailure() {
        TokenProvider tokenProvider = mock(TokenProvider.class);
        when(tokenProvider.verifyRefreshToken("malformed"))
                .thenThrow(new JWTDecodeException("parser detail"));

        assertThatThrownBy(() -> service(
                repository(),
                mock(SmsService.class),
                mock(BCryptPasswordEncoder.class),
                tokenProvider,
                mock(UserDetailsService.class)
        ).refreshAccessToken("malformed"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token is invalid");
    }

    @Test
    void disabledLockedAndUnknownUsersCannotRefresh() {
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(tokenProvider.verifyRefreshToken("disabled"))
                .thenReturn(decodedJWT);
        when(tokenProvider.verifyRefreshToken("locked"))
                .thenReturn(decodedJWT);
        when(tokenProvider.verifyRefreshToken("unknown"))
                .thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com"))
                .thenReturn(
                        principal(false, true),
                        principal(true, false)
                )
                .thenThrow(new UsernameNotFoundException("not found"));
        UserServiceImpl service = service(
                repository(),
                mock(SmsService.class),
                mock(BCryptPasswordEncoder.class),
                tokenProvider,
                userDetailsService
        );

        assertThatThrownBy(() -> service.refreshAccessToken("disabled"))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.refreshAccessToken("locked"))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.refreshAccessToken("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void normalizesEmailBeforeRequestingPasswordReset() {
        UserRepository<User> repository = repository();

        service(repository).requestPasswordReset(
                "  User.Name@EXAMPLE.COM  "
        );

        verify(repository).requestPasswordReset("user.name@example.com");
    }

    @Test
    void rejectsBlankAndInvalidResetEmail() {
        UserRepository<User> repository = repository();
        UserServiceImpl service = service(repository);

        assertThatThrownBy(() -> service.requestPasswordReset(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                () -> service.requestPasswordReset("not-an-email")
        ).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).requestPasswordReset(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void verifiesActiveTokenWithoutConsumingIt() {
        UserRepository<User> repository = repository();
        when(repository.findPasswordResetVerification(RESET_TOKEN))
                .thenReturn(activeVerification());

        var response = service(repository)
                .verifyPasswordResetToken("  " + RESET_TOKEN + "  ");

        assertThat(response.valid()).isTrue();
        verify(repository, never()).deletePasswordResetToken(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void rejectsBlankVerificationToken() {
        UserRepository<User> repository = repository();

        assertThatThrownBy(
                () -> service(repository).verifyPasswordResetToken(" ")
        ).isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(repository, never()).findPasswordResetVerification(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void rejectsExpiredVerificationTokenWithoutConsumingIt() {
        UserRepository<User> repository = repository();
        when(repository.findPasswordResetVerification(RESET_TOKEN))
                .thenThrow(new ExpiredPasswordResetTokenException("expired"));

        assertThatThrownBy(
                () -> service(repository)
                        .verifyPasswordResetToken(RESET_TOKEN)
        ).isInstanceOf(ExpiredPasswordResetTokenException.class);
        verify(repository, never()).deletePasswordResetToken(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void matchingPasswordsAreEncodedUpdatedAndTokenIsConsumed() {
        UserRepository<User> repository = repository();
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
        when(repository.findPasswordResetVerification(RESET_TOKEN))
                .thenReturn(activeVerification());
        when(encoder.encode("NewPassword123!"))
                .thenReturn("$2a$encoded-password");
        when(repository.updatePassword(42L, "$2a$encoded-password"))
                .thenReturn(1);
        when(repository.deletePasswordResetToken(42L, RESET_TOKEN))
                .thenReturn(1);

        service(
                repository,
                mock(SmsService.class),
                encoder
        ).resetPassword(resetRequest(
                RESET_TOKEN,
                "NewPassword123!",
                "NewPassword123!"
        ));

        var ordered = inOrder(repository);
        ordered.verify(repository).findPasswordResetVerification(RESET_TOKEN);
        ordered.verify(repository).updatePassword(42L, "$2a$encoded-password");
        ordered.verify(repository).deletePasswordResetToken(42L, RESET_TOKEN);
        verify(repository, never()).updatePassword(
                42L,
                "NewPassword123!"
        );
    }

    @Test
    void mismatchedOrBlankPasswordsAreRejectedBeforePersistence() {
        UserRepository<User> repository = repository();
        UserServiceImpl service = service(repository);

        assertThatThrownBy(() -> service.resetPassword(resetRequest(
                RESET_TOKEN,
                "NewPassword123!",
                "different"
        ))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.resetPassword(resetRequest(
                RESET_TOKEN,
                " ",
                " "
        ))).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).findPasswordResetVerification(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void tokenCannotBeReusedAfterSuccessfulReset() {
        UserRepository<User> repository = repository();
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
        when(repository.findPasswordResetVerification(RESET_TOKEN))
                .thenReturn(activeVerification())
                .thenThrow(new InvalidPasswordResetTokenException(
                        "already consumed"
                ));
        when(encoder.encode("NewPassword123!")).thenReturn("$2a$encoded");
        when(repository.updatePassword(42L, "$2a$encoded")).thenReturn(1);
        when(repository.deletePasswordResetToken(42L, RESET_TOKEN))
                .thenReturn(1);
        UserServiceImpl service = service(
                repository,
                mock(SmsService.class),
                encoder
        );
        PasswordResetRequest request = resetRequest(
                RESET_TOKEN,
                "NewPassword123!",
                "NewPassword123!"
        );

        service.resetPassword(request);

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void tokenDeletionFailurePropagatesForTransactionRollback() throws Exception {
        UserRepository<User> repository = repository();
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
        when(repository.findPasswordResetVerification(RESET_TOKEN))
                .thenReturn(activeVerification());
        when(encoder.encode("NewPassword123!")).thenReturn("$2a$encoded");
        when(repository.updatePassword(42L, "$2a$encoded")).thenReturn(1);
        when(repository.deletePasswordResetToken(42L, RESET_TOKEN))
                .thenReturn(0);

        assertThatThrownBy(() -> service(
                repository,
                mock(SmsService.class),
                encoder
        ).resetPassword(resetRequest(
                RESET_TOKEN,
                "NewPassword123!",
                "NewPassword123!"
        ))).isInstanceOf(IllegalStateException.class);

        assertThat(UserServiceImpl.class
                .getMethod("resetPassword", PasswordResetRequest.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    private PasswordResetVerification activeVerification() {
        return new PasswordResetVerification(
                42L,
                LocalDateTime.now().plusMinutes(5)
        );
    }

    private PasswordResetRequest resetRequest(
            String token,
            String password,
            String confirmation
    ) {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken(token);
        request.setPassword(password);
        request.setConfirmPassword(confirmation);
        return request;
    }

    private UserDTO userWithPhone(String phoneNumber) {
        UserDTO user = new UserDTO();
        user.setPhone(phoneNumber);
        return user;
    }

    private UserServiceImpl service(UserRepository<User> repository) {
        return service(
                repository,
                mock(SmsService.class),
                mock(BCryptPasswordEncoder.class)
        );
    }

    private UserServiceImpl service(
            UserRepository<User> repository,
            SmsService smsService,
            BCryptPasswordEncoder encoder
    ) {
        return service(
                repository,
                smsService,
                encoder,
                mock(TokenProvider.class),
                mock(UserDetailsService.class)
        );
    }

    private UserServiceImpl service(
            UserRepository<User> repository,
            SmsService smsService,
            BCryptPasswordEncoder encoder,
            TokenProvider tokenProvider,
            UserDetailsService userDetailsService
    ) {
        return new UserServiceImpl(
                repository,
                smsService,
                encoder,
                tokenProvider,
                userDetailsService
        );
    }

    private CustomerConnectUserPrincipal principal(
            boolean enabled,
            boolean notLocked
    ) {
        return new CustomerConnectUserPrincipal(
                User.builder()
                        .id(42L)
                        .email("user@example.com")
                        .password("encoded-password")
                        .enabled(enabled)
                        .isNotLocked(notLocked)
                        .build(),
                "READ:USER"
        );
    }

    @SuppressWarnings("unchecked")
    private UserRepository<User> repository() {
        return mock(UserRepository.class);
    }
}

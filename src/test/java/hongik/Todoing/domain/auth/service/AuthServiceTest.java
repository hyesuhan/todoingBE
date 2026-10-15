package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.dto.SignUpRequestDto;
import hongik.Todoing.domain.auth.jwt.JwtUtil;
import hongik.Todoing.domain.auth.adaptor.KakaoAdaptor;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String NICKNAME = "nickname";
    private static final String PASSWORD = "password";

    @Mock
    private KakaoAdaptor kakaoAdaptor;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationQueryService emailVerificationQueryService;
    @Mock
    private EmailVerificationCommandService emailVerificationCommandService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                kakaoAdaptor, userRepository, jwtUtil, passwordEncoder,
                emailVerificationQueryService, emailVerificationCommandService);
    }

    private SignUpRequestDto signUpRequest() {
        SignUpRequestDto request = new SignUpRequestDto();
        ReflectionTestUtils.setField(request, "nickname", NICKNAME);
        ReflectionTestUtils.setField(request, "email", EMAIL);
        ReflectionTestUtils.setField(request, "password", PASSWORD);
        return request;
    }

    @Test
    void signUpByEmail_whenEmailNotVerified_shouldThrowEmailNotVerified() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> authService.signUpByEmail(signUpRequest()))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_NOT_VERIFIED);

        verify(userRepository, never()).save(any());
    }

    @Test
    void signUpByEmail_whenEmailAlreadyRegistered_shouldThrowEmailDuplicated() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.signUpByEmail(signUpRequest()))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_DUPLICATED);
    }

    @Test
    void signUpByEmail_whenNicknameAlreadyRegistered_shouldThrowNicknameDuplicated() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByNickname(NICKNAME)).thenReturn(true);

        assertThatThrownBy(() -> authService.signUpByEmail(signUpRequest()))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_NICKNAME_DUPLICATED);
    }

    @Test
    void signUpByEmail_whenVerifiedAndNotDuplicated_shouldSaveUserAndConsumeVerification() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByNickname(NICKNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");

        authService.signUpByEmail(signUpRequest());

        verify(userRepository).save(any(User.class));
        verify(emailVerificationCommandService).consumeVerification(EMAIL);
    }

    @Test
    void signUpByEmail_whenConsumeVerificationFails_shouldNotPropagateSinceUserAlreadySaved() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByNickname(NICKNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
        doThrow(new RuntimeException("redis down"))
                .when(emailVerificationCommandService).consumeVerification(EMAIL);

        assertThatCode(() -> authService.signUpByEmail(signUpRequest())).doesNotThrowAnyException();

        verify(userRepository).save(any(User.class));
    }
}

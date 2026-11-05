package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.adaptor.KakaoAdaptor;
import hongik.Todoing.domain.auth.dto.KakaoDTO;
import hongik.Todoing.domain.auth.dto.SignUpRequestDto;
import hongik.Todoing.domain.auth.jwt.JwtUtil;
import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.service.UserQueryService;
import hongik.Todoing.domain.user.service.UserService;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    private UserQueryService userQueryService;
    @Mock
    private UserService userService;
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
                kakaoAdaptor, userQueryService, userService, jwtUtil, passwordEncoder,
                emailVerificationQueryService, emailVerificationCommandService);
    }

    private SignUpRequestDto signUpRequest() {
        SignUpRequestDto request = new SignUpRequestDto();
        ReflectionTestUtils.setField(request, "nickname", NICKNAME);
        ReflectionTestUtils.setField(request, "email", EMAIL);
        ReflectionTestUtils.setField(request, "password", PASSWORD);
        return request;
    }

    private KakaoDTO.KakaoProfile kakaoProfile(String email) {
        KakaoDTO.KakaoProfile kakaoProfile = mock(KakaoDTO.KakaoProfile.class);
        KakaoDTO.KakaoProfile.KakaoAccount kakaoAccount = mock(KakaoDTO.KakaoProfile.KakaoAccount.class);

        when(kakaoProfile.getKakao_account()).thenReturn(kakaoAccount);
        when(kakaoAccount.getEmail()).thenReturn(email);

        return kakaoProfile;
    }

    private KakaoDTO.KakaoProfile kakaoProfile(String email, String nickname) {
        KakaoDTO.KakaoProfile kakaoProfile = kakaoProfile(email);
        KakaoDTO.KakaoProfile.Properties properties = mock(KakaoDTO.KakaoProfile.Properties.class);

        when(kakaoProfile.getProperties()).thenReturn(properties);
        when(properties.getNickname()).thenReturn(nickname);

        return kakaoProfile;
    }

    @Test
    void loginByOAuth_whenUserExists_shouldReturnExistingUserWithoutCreating() {
        KakaoDTO.OAuthToken oAuthToken = mock(KakaoDTO.OAuthToken.class);
        KakaoDTO.KakaoProfile kakaoProfile = kakaoProfile(EMAIL);
        User existingUser = User.create(NICKNAME, EMAIL, "encoded", Role.USER);

        when(kakaoAdaptor.requestToken("code")).thenReturn(oAuthToken);
        when(kakaoAdaptor.requestProfile(oAuthToken)).thenReturn(kakaoProfile);
        when(userQueryService.findOptionalByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(jwtUtil.createAccessToken(EMAIL, "ROLE_USER")).thenReturn("access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        User result = authService.loginByOAuth("code", response);

        assertThat(result).isEqualTo(existingUser);
        assertThat(response.getHeader("Authorization")).isEqualTo("access-token");
        verify(userService, never()).save(any());
    }

    @Test
    void loginByOAuth_whenUserDoesNotExist_shouldCreateNewUser() {
        KakaoDTO.OAuthToken oAuthToken = mock(KakaoDTO.OAuthToken.class);
        KakaoDTO.KakaoProfile kakaoProfile = kakaoProfile(EMAIL, NICKNAME);

        when(kakaoAdaptor.requestToken("code")).thenReturn(oAuthToken);
        when(kakaoAdaptor.requestProfile(oAuthToken)).thenReturn(kakaoProfile);
        when(userQueryService.findOptionalByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("OAUTH")).thenReturn("encoded");
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.createAccessToken(EMAIL, "ROLE_USER")).thenReturn("access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        User result = authService.loginByOAuth("code", response);

        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getNickname()).isEqualTo(NICKNAME);
        verify(userService).save(any(User.class));
    }

    @Test
    void loginByEmail_whenUserNotFound_shouldThrowMemberNotFound() {
        when(userQueryService.findByEmail(EMAIL))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> authService.loginByEmail(EMAIL, PASSWORD))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
    }

    @Test
    void loginByEmail_whenCredentialsValid_shouldReturnJwtTokens() {
        User user = User.create(NICKNAME, EMAIL, "encoded", Role.USER);
        user.verifyEmail();

        when(userQueryService.findByEmail(EMAIL)).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, "encoded")).thenReturn(true);
        when(jwtUtil.createJwtAccessToken(any())).thenReturn("access");
        when(jwtUtil.createJwtRefreshToken(any())).thenReturn("refresh");

        var result = authService.loginByEmail(EMAIL, PASSWORD);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void signUpByEmail_whenEmailNotVerified_shouldThrowEmailNotVerified() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> authService.signUpByEmail(signUpRequest()))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_NOT_VERIFIED);

        verify(userService, never()).save(any());
    }

    @Test
    void signUpByEmail_whenEmailAlreadyRegistered_shouldThrowEmailDuplicated() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userQueryService.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.signUpByEmail(signUpRequest()))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_DUPLICATED);
    }

    @Test
    void signUpByEmail_whenNicknameAlreadyRegistered_shouldThrowNicknameDuplicated() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userQueryService.existsByEmail(EMAIL)).thenReturn(false);
        when(userQueryService.existsByNickname(NICKNAME)).thenReturn(true);

        assertThatThrownBy(() -> authService.signUpByEmail(signUpRequest()))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.EMAIL_NICKNAME_DUPLICATED);
    }

    @Test
    void signUpByEmail_whenVerifiedAndNotDuplicated_shouldSaveUserAndConsumeVerification() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userQueryService.existsByEmail(EMAIL)).thenReturn(false);
        when(userQueryService.existsByNickname(NICKNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");

        authService.signUpByEmail(signUpRequest());

        verify(userService).save(any(User.class));
        verify(emailVerificationCommandService).consumeVerification(EMAIL);
    }

    @Test
    void signUpByEmail_whenConsumeVerificationFails_shouldNotPropagateSinceUserAlreadySaved() {
        when(emailVerificationQueryService.isVerified(EMAIL)).thenReturn(true);
        when(userQueryService.existsByEmail(EMAIL)).thenReturn(false);
        when(userQueryService.existsByNickname(NICKNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
        doThrow(new RuntimeException("redis down"))
                .when(emailVerificationCommandService).consumeVerification(EMAIL);

        assertThatCode(() -> authService.signUpByEmail(signUpRequest())).doesNotThrowAnyException();

        verify(userService).save(any(User.class));
    }
}

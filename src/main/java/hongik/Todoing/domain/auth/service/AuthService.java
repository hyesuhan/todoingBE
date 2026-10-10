package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.converter.AuthConverter;
import hongik.Todoing.domain.auth.dto.SignUpRequestDto;
import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.auth.jwt.JwtUtil;
import hongik.Todoing.domain.auth.util.KakaoUtil;
import hongik.Todoing.domain.auth.dto.KakaoDTO;
import hongik.Todoing.domain.auth.jwt.dto.JwtDTO;
import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.Status;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoUtil kakaoUtil;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;


    public User loginByOAuth(String accessCode, HttpServletResponse response) {
        KakaoDTO.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
        KakaoDTO.KakaoProfile kakaoProfile= kakaoUtil.requestProfile(oAuthToken);

        String email = kakaoProfile.getKakao_account().getEmail();

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(kakaoProfile));

        String token = jwtUtil.createAccessToken(user.getEmail(), "ROLE_" + user.getRole().name());
        response.setHeader("Authorization", token);

        return user;
    }

    private User createNewUser(KakaoDTO.KakaoProfile kakaoProfile) {
        User newUser = AuthConverter.toUser(
                kakaoProfile.getKakao_account().getEmail(),
                kakaoProfile.getProperties().getNickname(),
                "OAUTH",
                passwordEncoder
        );

        return userRepository.save(newUser);
    }

    public JwtDTO loginByEmail(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (user.getStatus() == Status.WITHDRAWN) {
            throw new GeneralException(ErrorStatus.USER_ALREADY_WITHDRAWN);
        }

        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new GeneralException(ErrorStatus.INVALID_PASSWORD);
        }

        // PrincipalDetails 생성
        PrincipalDetails principalDetails = new PrincipalDetails(user);

        // JWT 발급
        String accessToken = jwtUtil.createJwtAccessToken(principalDetails);
        String refreshToken = jwtUtil.createJwtRefreshToken(principalDetails);

        return new JwtDTO(accessToken, refreshToken);
    }

    public void signUpByEmail(SignUpRequestDto request) {

        if(userRepository.existsByEmailAndNickname(request.getEmail(), request.getNickname())) {
            throw new GeneralException(ErrorStatus.EMAIL_NICKNAME_DUPLICATED);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // User 생성
        User user = User.create(request.getNickname(), request.getEmail(), encodedPassword, Role.USER);

        userRepository.save(user);

    }
}

package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.converter.AuthConverter;
import hongik.Todoing.domain.auth.dto.SignUpRequestDto;
import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.jwt.JwtUtil;
import hongik.Todoing.domain.auth.util.KakaoUtil;
import hongik.Todoing.domain.auth.dto.KakaoDTO;
import hongik.Todoing.domain.jwt.dto.JwtDTO;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.member.repository.MemberRepository;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoUtil kakaoUtil;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;


    public User loginByOAuth(String accessCode, HttpServletResponse response) {
        KakaoDTO.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
        KakaoDTO.KakaoProfile kakaoProfile= kakaoUtil.requestProfile(oAuthToken);

        String email = kakaoProfile.getKakao_account().getEmail();

        User user = memberRepository.findByEmail(email)
                .orElseGet(() -> createNewMember(kakaoProfile));

        String token = jwtUtil.createAccessToken(user.getEmail(), user.getRole().toString());
        response.setHeader("Authorization", token);

        return user;
    }

    private User createNewMember(KakaoDTO.KakaoProfile kakaoProfile) {
        User newUser = AuthConverter.toMember(
                kakaoProfile.getKakao_account().getEmail(),
                kakaoProfile.getProperties().getNickname(),
                "OAUTH",
                passwordEncoder
        );

        return memberRepository.save(newUser);
    }

    public JwtDTO loginByEmail(String email, String password) {
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

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
        // 이메일 중복 체크
        if(memberRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(ErrorStatus.EMAIL_DUPLICATED);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Member 생성
        User user = User.builder()
                .nickname(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .role("ROLE_USER")
                .build();

        memberRepository.save(user);

        // 자동 로그인 안 함

    }
}

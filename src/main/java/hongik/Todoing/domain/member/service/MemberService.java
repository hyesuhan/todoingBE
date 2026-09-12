package hongik.Todoing.domain.member.service;

import hongik.Todoing.domain.member.converter.MemberConverter;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.member.dto.response.GetProfileDTO;
import hongik.Todoing.domain.member.dto.response.UpdateProfileDTO;
import hongik.Todoing.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    public GetProfileDTO getProfile(User user) {
        return MemberConverter.toGetProfileDTO(user.getEmail(), user.getName());
    }


    @Transactional
    public void updateProfile(User user, UpdateProfileDTO request) {
        if(request.name() != null)
            user.updateName(request.name());

        if(request.password() != null) {
            String encoded = passwordEncoder.encode(request.password());
            user.updatePassword(encoded);
        }

        memberRepository.save(user);

    }
}

package hongik.Todoing.domain.user.service;

import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.dto.response.GetProfileDTO;
import hongik.Todoing.domain.user.dto.response.UpdateProfileDTO;
import hongik.Todoing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserCacheService userCacheService;

    public GetProfileDTO getProfile(User user) {
        return GetProfileDTO.from(user);
    }

    @Transactional
    public void updateProfile(User user, UpdateProfileDTO request) {
        if(request.nickname() != null)
            user.updateNickname(request.nickname());

        if(request.password() != null) {
            String encoded = passwordEncoder.encode(request.password());
            user.updatePassword(encoded);
        }

        userRepository.save(user);
        userCacheService.evict(user.getEmail());
    }
}

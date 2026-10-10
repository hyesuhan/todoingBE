package hongik.Todoing.domain.auth.service;

import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrincipalDetailService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<User> memberEntity = userRepository.findByEmail(email);
        if(memberEntity.isPresent()) {
            User user = memberEntity.get();
            //return new PrincipalDetails(member.getName(), member.getPassword(), member.getRole());
            return new PrincipalDetails(user);
        }
        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}

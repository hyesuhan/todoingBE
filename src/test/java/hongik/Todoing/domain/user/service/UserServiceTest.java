package hongik.Todoing.domain.user.service;

import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCacheService userCacheService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(passwordEncoder, userRepository, userCacheService);
    }

    @Test
    void save_delegatesToRepository() {
        User user = User.create("nickname", "user@example.com", "encodedPassword", Role.USER);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);

        assertThat(result).isEqualTo(user);
    }
}

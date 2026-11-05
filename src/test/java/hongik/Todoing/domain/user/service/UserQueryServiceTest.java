package hongik.Todoing.domain.user.service;

import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String NICKNAME = "nickname";
    private static final Long ID = 1L;

    @Mock
    private UserRepository userRepository;

    private UserQueryService userQueryService;

    private User user;

    @BeforeEach
    void setUp() {
        userQueryService = new UserQueryService(userRepository);
        user = User.create(NICKNAME, EMAIL, "encodedPassword", Role.USER);
    }

    @Test
    void findByEmail_whenExists_shouldReturnUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        User result = userQueryService.findByEmail(EMAIL);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void findByEmail_whenMissing_shouldThrowGeneralException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.findByEmail(EMAIL))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void findOptionalByEmail_delegatesToRepository() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<User> result = userQueryService.findOptionalByEmail(EMAIL);

        assertThat(result).contains(user);
    }

    @Test
    void findOptionalByEmail_whenMissing_shouldReturnEmpty() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        Optional<User> result = userQueryService.findOptionalByEmail(EMAIL);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_whenExists_shouldReturnUser() {
        when(userRepository.findById(ID)).thenReturn(Optional.of(user));

        User result = userQueryService.findById(ID);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void findById_whenMissing_shouldThrowGeneralException() {
        when(userRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.findById(ID))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void existsByEmail_delegatesToRepository() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        boolean result = userQueryService.existsByEmail(EMAIL);

        assertThat(result).isTrue();
    }

    @Test
    void existsByNickname_delegatesToRepository() {
        when(userRepository.existsByNickname(NICKNAME)).thenReturn(true);

        boolean result = userQueryService.existsByNickname(NICKNAME);

        assertThat(result).isTrue();
    }
}

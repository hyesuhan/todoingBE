package hongik.Todoing.domain.friend.service;

import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.domain.friend.domain.FriendStatus;
import hongik.Todoing.domain.friend.repository.FriendRepository;
import hongik.Todoing.domain.todo.converter.TodoConverter;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.response.TodoResponseDTO;
import hongik.Todoing.domain.todo.service.TodoQueryService;
import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.service.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private TodoQueryService todoQueryService;

    @Mock
    private TodoConverter todoConverter;

    private FriendService friendService;

    private User me;
    private User target;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(userQueryService, friendRepository, todoQueryService, todoConverter);
        me = User.create("me", "me@example.com", "encoded", Role.USER);
        target = User.create("target", "target@example.com", "encoded", Role.USER);
    }

    @Test
    void addFriend_savesFriendshipWhenNotDuplicated() {
        when(userQueryService.findByEmail("target@example.com")).thenReturn(target);
        when(friendRepository.existsByUserAndFriend(me, target)).thenReturn(false);

        friendService.addFriend(me, "target@example.com");

        verify(friendRepository).save(org.mockito.ArgumentMatchers.any(Friend.class));
    }

    @Test
    void deleteFriend_removesFriendship() {
        Friend friend = Friend.of(me, target);
        when(userQueryService.findById(1L)).thenReturn(target);
        when(friendRepository.findByUserAndFriend(me, target)).thenReturn(friend);

        friendService.deleteFriend(me, 1L);

        verify(friendRepository).delete(friend);
    }

    @Test
    void blockFriend_updatesStatusToBlocked() {
        Friend friend = Friend.of(me, target);
        when(userQueryService.findById(1L)).thenReturn(target);
        when(friendRepository.findByUserAndFriend(me, target)).thenReturn(friend);

        friendService.blockFriend(me, 1L);

        assertThat(friend.getStatus()).isEqualTo(FriendStatus.BLOCKED);
    }

    @Test
    void getFriendTodos_returnsConvertedTodosForActiveFriend() {
        Friend friend = Friend.of(me, target);
        List<Todo> todos = List.of();
        List<TodoResponseDTO> expected = List.of();
        when(userQueryService.findById(1L)).thenReturn(target);
        when(friendRepository.findByUserAndFriend(me, target)).thenReturn(friend);
        when(todoQueryService.findAllByUserId(target.getId())).thenReturn(todos);
        when(todoConverter.toTodoDtoList(todos)).thenReturn(expected);

        List<TodoResponseDTO> result = friendService.getFriendTodos(me, 1L);

        assertThat(result).isEqualTo(expected);
    }
}

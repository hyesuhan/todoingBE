package hongik.Todoing.domain.friend.service;

import hongik.Todoing.domain.friend.converter.FriendConverter;
import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.domain.friend.domain.FriendStatus;
import hongik.Todoing.domain.friend.dto.FriendResponseDTO;
import hongik.Todoing.domain.friend.repository.FriendRepository;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.service.UserQueryService;
import hongik.Todoing.domain.todo.converter.TodoConverter;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.response.TodoResponseDTO;
import hongik.Todoing.domain.todo.service.TodoQueryService;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final UserQueryService userQueryService;
    private final FriendRepository friendRepository;
    private final TodoQueryService todoQueryService;
    private final TodoConverter todoConverter;

    @Transactional
    public void addFriend(User me, String friendEmail) {

        User target = userQueryService.findByEmail(friendEmail);

        if (friendRepository.existsByUserAndFriend(me, target)) {
            throw new GeneralException(ErrorStatus.FRIEND_REQUEST_DUPLICATED);
        }

        Friend friendship = me.createFriendship(target);
        friendRepository.save(friendship);

    }

    public List<FriendResponseDTO> getMyFriends(User me) {
        List<Friend> friends = friendRepository.findAllByUser(me);
        return FriendConverter.toFrienResponseDtoList(friends);
    }

    public void deleteFriend(User me, Long friendId) {
        User target = userQueryService.findById(friendId);

        Friend friend = friendRepository.findByUserAndFriend(me, target);

        friendRepository.delete(friend);

    }

    public void blockFriend(User me, Long friendId) {
        User target = userQueryService.findById(friendId);

        Friend friend = friendRepository.findByUserAndFriend(me, target);

        friend.updateStatus(FriendStatus.BLOCKED);
    }

    public List<TodoResponseDTO> getFriendTodos(User me, Long friendId) {
        User target = userQueryService.findById(friendId);

        Friend friend = friendRepository.findByUserAndFriend(me, target);

        if(friend.getStatus() == FriendStatus.BLOCKED) {
            throw new GeneralException(ErrorStatus.FRIEND_BLOCKED);
        }

        List<Todo> todos = todoQueryService.findAllByUserId(target.getId());
        return todoConverter.toTodoDtoList(todos);
    }

}

package hongik.Todoing.domain.friend.service;

import hongik.Todoing.domain.friend.converter.FriendConverter;
import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.domain.friend.domain.FriendStatus;
import hongik.Todoing.domain.friend.dto.FriendResponseDTO;
import hongik.Todoing.domain.friend.repository.FriendRepository;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.domain.user.repository.UserRepository;
import hongik.Todoing.domain.todo.converter.TodoConverter;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.dto.response.TodoResponseDTO;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final TodoRepository todoRepository;
    private final TodoConverter todoConverter;

    @Transactional
    public void addFriend(User me, String friendEmail) {

        User target = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

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
        User target = userRepository.findById(friendId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Friend friend = friendRepository.findByUserAndFriend(me, target);

        friendRepository.delete(friend);

    }

    public void blockFriend(User me, Long friendId) {
        User target = userRepository.findById(friendId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Friend friend = friendRepository.findByUserAndFriend(me, target);

        friend.updateStatus(FriendStatus.BLOCKED);
    }

    public List<TodoResponseDTO> getFriendTodos(User me, Long friendId) {
        User target = userRepository.findById(friendId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Friend friend = friendRepository.findByUserAndFriend(me, target);

        if(friend.getStatus() == FriendStatus.BLOCKED) {
            throw new GeneralException(ErrorStatus.FRIEND_BLOCKED);
        }

        List<Todo> todos = todoRepository.findByMemberId(target.getId());
        return todoConverter.toTodoDtoList(todos);
    }

}

package hongik.Todoing.domain.friend.converter;

import hongik.Todoing.domain.friend.domain.Friend;
import hongik.Todoing.domain.friend.dto.FriendResponseDTO;
import hongik.Todoing.domain.member.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FriendConverter {

    public static FriendResponseDTO toFriendResponse(Friend friend) {
        User target = friend.getFriend();
        return new FriendResponseDTO(
                target.getId(),
                target.getName(),
                friend.getStatus());
    }

    public static List<FriendResponseDTO> toFrienResponseDtoList(List<Friend> friends) {
        return friends.stream()
                .map(FriendConverter::toFriendResponse)
                .toList();
    }
}

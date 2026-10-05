package hongik.Todoing.domain.friend.dto;

import hongik.Todoing.domain.friend.domain.FriendStatus;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public record FriendResponseDTO(Long id, String friendName, FriendStatus status) {
    public FriendResponseDTO {
        if (friendName == null) {
            throw new GeneralException(ErrorStatus.FRIEND_REQUEST_IS_NULL);
        }
        if(status == FriendStatus.BLOCKED) {
            throw new GeneralException(ErrorStatus.FRIEND_BLOCKED);
        }
    }
}

package hongik.Todoing.domain.aiChat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatResultDTO {
    private String id;      // 고유 ID
    private String type;
    private String content; // 프론트가 보여줄 메시지
}

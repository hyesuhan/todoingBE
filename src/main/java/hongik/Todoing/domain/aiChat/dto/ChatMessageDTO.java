package hongik.Todoing.domain.aiChat.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO implements Serializable {
    private String role;
    private String content;
}

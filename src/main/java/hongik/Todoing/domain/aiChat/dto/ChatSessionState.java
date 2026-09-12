package hongik.Todoing.domain.aiChat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionState implements Serializable {
    private String category;
    private String startDate;
    private String endDate;
    private String level;

}

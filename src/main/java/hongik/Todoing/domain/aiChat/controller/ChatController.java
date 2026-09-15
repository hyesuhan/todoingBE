package hongik.Todoing.domain.aiChat.controller;

import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.aiChat.dto.request.ChatRequestDTO;
import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.domain.aiChat.dto.response.ChatResultDTO;
import hongik.Todoing.domain.aiChat.dto.response.ChatSessionCreateResponseDTO;
import hongik.Todoing.domain.aiChat.dto.response.ChatSessionSummaryDTO;
import hongik.Todoing.domain.aiChat.dto.response.ChatSubmitResponseDTO;
import hongik.Todoing.domain.aiChat.service.ChatDebounceService;
import hongik.Todoing.domain.aiChat.service.ChatSessionService;
import hongik.Todoing.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionService chatSessionService;
    private final ChatDebounceService chatDebounceService;

    @PostMapping("/setting")
    public ApiResponse<ChatSessionCreateResponseDTO> setting(@AuthenticationPrincipal PrincipalDetails principal,
                          @RequestBody ChatSessionState requestDTO){

        String sessionId = UUID.randomUUID().toString();
        String key = principal.getUsername() + ":" + sessionId;

        chatSessionService.save(key, requestDTO);
        chatSessionService.registerSession(principal.getUsername(), sessionId);
        return ApiResponse.onSuccess(new ChatSessionCreateResponseDTO(sessionId));

    }

    @PostMapping("/{sessionId}/message")
    public ApiResponse<ChatSubmitResponseDTO> chat(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable String sessionId,
            @RequestBody ChatRequestDTO requestDTO) {

        String key = principal.getUsername() + ":" + sessionId;
        chatDebounceService.receiveUserMessage(key, requestDTO.getMessages().get(0));
        return ApiResponse.onSuccess(ChatSubmitResponseDTO.of(sessionId));

    }

    @GetMapping("/{sessionId}/result")
    public ApiResponse<ChatResultDTO> getResult(@AuthenticationPrincipal PrincipalDetails principal,
                                                 @PathVariable String sessionId) {
        String key = principal.getUsername() + ":" + sessionId;
        ChatResultDTO result = chatDebounceService.getResult(key);
        return ApiResponse.onSuccess(result);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionSummaryDTO>> listSessions(@AuthenticationPrincipal PrincipalDetails principal) {
        return ApiResponse.onSuccess(chatSessionService.listSessions(principal.getUsername()));
    }

}

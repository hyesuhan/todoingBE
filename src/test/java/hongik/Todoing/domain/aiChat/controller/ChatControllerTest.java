package hongik.Todoing.domain.aiChat.controller;

import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.domain.aiChat.dto.response.ChatSessionCreateResponseDTO;
import hongik.Todoing.domain.aiChat.service.ChatDebounceService;
import hongik.Todoing.domain.aiChat.service.ChatSessionService;
import hongik.Todoing.domain.auth.security.PrincipalDetails;
import hongik.Todoing.domain.order.exception.passException.PassNotFoundException;
import hongik.Todoing.domain.order.facade.PassUsecase;
import hongik.Todoing.domain.user.domain.Role;
import hongik.Todoing.domain.user.domain.User;
import hongik.Todoing.infrastructure.apiPayload.ApiResponse;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock private ChatSessionService chatSessionService;
    @Mock private ChatDebounceService chatDebounceService;
    @Mock private PassUsecase passUsecase;
    private ChatController chatController;
    private PrincipalDetails principal;
    private ChatSessionState requestDTO;

    @BeforeEach
    void setUp() {
        chatController = new ChatController(chatSessionService, chatDebounceService, passUsecase);
        User user = User.create("nick", "user@example.com", "pw", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        principal = new PrincipalDetails(user);
        requestDTO = new ChatSessionState("category", "2026-01-01", "2026-01-07", "easy");
    }

    @Test
    void setting_whenPassAvailable_shouldConsumePassBeforeSavingSession() {
        ApiResponse<ChatSessionCreateResponseDTO> response = chatController.setting(principal, requestDTO);

        InOrder inOrder = inOrder(passUsecase, chatSessionService);
        inOrder.verify(passUsecase).consumePass(1L);
        inOrder.verify(chatSessionService).save(anyString(), eq(requestDTO));
        inOrder.verify(chatSessionService).registerSession(eq("user@example.com"), anyString());
        assertThat(response.getResult().sessionId()).isNotBlank();
    }

    @Test
    void setting_whenNoPassAvailable_shouldThrowAndNotSaveOrRegisterSession() {
        doThrow(PassNotFoundException.EXCEPTION).when(passUsecase).consumePass(1L);

        assertThatThrownBy(() -> chatController.setting(principal, requestDTO))
                .isInstanceOf(GeneralException.class);

        verify(chatSessionService, never()).save(any(), any());
        verify(chatSessionService, never()).registerSession(any(), any());
    }
}

package hongik.Todoing.domain.todo.controller;


import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.domain.aiChat.service.ChatSessionService;
import hongik.Todoing.domain.todo.dto.request.ChatTodoCreateRequestDTO;
import hongik.Todoing.domain.todo.dto.request.TodoCreateRequestDTO;
import hongik.Todoing.domain.todo.dto.request.TodoUpdateRequestDTO;
import hongik.Todoing.domain.todo.dto.response.TodoResponseDTO;
import hongik.Todoing.domain.todo.facade.TodoUsecase;
import hongik.Todoing.domain.todo.service.ChatTodoService;
import hongik.Todoing.domain.todo.service.SelfTodoService;
import hongik.Todoing.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final SelfTodoService selfTodoService;
    private final ChatSessionService chatSessionService;
    private final ChatTodoService chatTodoService;
    private final TodoUsecase todoUsecase;

    // 투두리스트 만들기 - self
    @Operation(summary = "투두리스트를 생성합니다.")
    @PostMapping
    public ApiResponse<Void> createTodo(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody TodoCreateRequestDTO requestDTO
            ) {
        todoUsecase.createTodo(principal.getUser().getId(), requestDTO);
        return ApiResponse.onSuccess(null);
    }

    // 투두리스트 보기 - 날짜로
    @Operation(summary = "투두리스트를 날짜로 조회합니다.")
    @GetMapping
    public ApiResponse<List<TodoResponseDTO>> getMyToos(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date
            ) {
        List<TodoResponseDTO> todos = selfTodoService.getTodosByDate(principal.getUser().getId(), date);
        return ApiResponse.onSuccess(todos);
    }


    // 투두 삭제하기
    @Operation(summary = "투두리스트를 삭제합니다.")
    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> deleteTodo(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long todoId
    ) {
        selfTodoService.deleteTodo(principal.getUser().getId(), todoId);
        return ApiResponse.onSuccess(null);
    }


    // 투두 수정하기
    @Operation(summary = "투두리스트를 수정합니다.")
    @PutMapping("/{todoId}")
    public ApiResponse<Void> updateTodo(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long todoId,
            @RequestBody TodoUpdateRequestDTO requestDTO
    ) {
        selfTodoService.updateTodo(principal.getUser().getId(), todoId, requestDTO);
        return ApiResponse.onSuccess(null);
    }

    // 투두 완료-미완료 토글
    @Operation(summary = "투두리스트의 성공 여부 토글")
    @PutMapping("/{todoId}/toggle")
    public ApiResponse<Void> toggleComplete(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long todoId
    ) {
        selfTodoService.toggleTodo(principal.getUser().getId(), todoId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "챗봇과 함께 투두리스트를 생성합니다.")
    @PostMapping("/chat")
    public ApiResponse<Void> createTodoWithChat(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam String sessionId,
            @RequestBody ChatTodoCreateRequestDTO requestDTO){

        String key = principal.getUsername() + ":" + sessionId;
        ChatSessionState sessionState = chatSessionService.get(key);
        if(sessionState == null ){
            return ApiResponse.onSuccess(null);
        }
        chatTodoService.createTodo(principal.getUser(), requestDTO, sessionState);
        return ApiResponse.onSuccess(null);

    }


}

package hongik.Todoing.domain.verification.controller;

import com.google.cloud.vision.v1.EntityAnnotation;
import hongik.Todoing.domain.auth.util.PrincipalDetails;
import hongik.Todoing.domain.verification.dto.TextAnnotationDto;
import hongik.Todoing.domain.verification.dto.VerificationResponse;
import hongik.Todoing.domain.verification.service.SpeechService;
import hongik.Todoing.domain.verification.service.VerificationService;
import hongik.Todoing.domain.verification.service.VisionService;
import hongik.Todoing.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verification")
public class TodoVerificationController {

    private final VisionService visionService;
    private final VerificationService verificationService;
    private final SpeechService speechService;

    /*
    - 사진으로 인증하기 -> 1. 사진 업로드 -> 2. 사진에서 글자 인식 -> 3. 인식된 글자가 할 일의 인증 문구와 일치하는지 확인
    -
     */

    @Operation(summary = "사진 인증 요청 컨트롤러")
    @PostMapping("/{todoId}/vision")
    public ApiResponse<?> verifyTextInImage(@PathVariable Long todoId,
                                            @RequestParam("image")MultipartFile image) throws IOException {
        List<EntityAnnotation> texts = visionService.detectText(image);

        if(!texts.isEmpty()) {
            String detetedText = texts.get(0).getDescription();
            System.out.println("결과로 나온 글은 " + detetedText);
        }
        return ApiResponse.onSuccess(texts);
    }

    @Operation(summary = "사진 인증 - 글 감지 테스트 컨트롤러")
    @PostMapping("/test")
    public ApiResponse<?> verifyTextTest(@RequestPart("image")MultipartFile image) throws IOException {
        List<EntityAnnotation> texts = visionService.detectText(image);

        List<TextAnnotationDto> result = texts.stream()
                .map(t -> new TextAnnotationDto(t.getDescription(), t.getLocale()))
                .toList();

        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "[x]사진 인증 - 라벨 감지 테스트 컨트롤러")
    @PostMapping("/test/labels")
    public ApiResponse<?> detectLabelsTest(@RequestPart("image")MultipartFile image) throws IOException {
        List<EntityAnnotation> labels = visionService.detectLabels(image);

        List<String> labelDescriptions = labels.stream()
                .map(EntityAnnotation::getDescription)
                .toList();

        System.out.println(labelDescriptions);

        return ApiResponse.onSuccess(labelDescriptions);
    }

    @Operation(summary = "[x]사진 인증, 글과 사진 어떤 것이든 사진은 한 번에 묶어서 인증이 가능합니다.")
    @PostMapping("/image")
    public ApiResponse<?> verifyTodoImage(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam Long todoId,
            @RequestPart("image") MultipartFile image
    ){
        VerificationResponse response = verificationService.verifyTodoImage(principal.getUser(), todoId, image);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "음성 인증 - STT 결과 transcript를 기반으로 투두 인증")
    @PostMapping("/voice")
    public ApiResponse<?> verifyTodoVoice(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam Long todoId,
            @RequestParam String transcript
    ) {
        VerificationResponse response =
                verificationService.verifyTodoVoice(principal.getUser(), todoId, transcript);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "텍스트 인증 - 사용자가 작성한 텍스트(50자 내외)로 투두 인증")
    @PostMapping("/text")
    public ApiResponse<?> verifyTodoText(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam Long todoId,
            @RequestParam String text
    ) {
        VerificationResponse response =
                verificationService.verifyTodoText(principal.getUser(), todoId, text);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/voice/file")
    public ApiResponse<?> verifyTodoVoiceFile(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam Long todoId,
            @RequestPart("audio") MultipartFile audioFile
    ) throws IOException {
        String transcript = speechService.stt(audioFile); // 음성 → 텍스트
        VerificationResponse response =
                verificationService.verifyTodoVoice(principal.getUser(), todoId, transcript);
        return ApiResponse.onSuccess(response);
    }

}

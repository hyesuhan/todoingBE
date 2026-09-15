package hongik.Todoing.domain.verification.service;

import com.google.cloud.vision.v1.EntityAnnotation;
import hongik.Todoing.domain.label.domain.LabelType;
import hongik.Todoing.domain.label.repository.LabelRepository;
import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.order.adaptor.PassAdaptor;
import hongik.Todoing.domain.order.domain.pass.Pass;
import hongik.Todoing.domain.order.validator.PassValidator;
import hongik.Todoing.domain.todo.domain.Todo;
import hongik.Todoing.domain.todo.repository.TodoRepository;
import hongik.Todoing.domain.verification.Adaptor.VerificationAdaptor;
import hongik.Todoing.domain.verification.domain.Verification;
import hongik.Todoing.domain.verification.domain.VerificationType;
import hongik.Todoing.domain.verification.domain.VerificationUsage;
import hongik.Todoing.domain.verification.dto.VerificationResponse;
import hongik.Todoing.domain.verification.dto.VerificationResult;
import hongik.Todoing.domain.verification.repository.VerificationUsageRepository;
import hongik.Todoing.domain.verification.validator.VerificationValidator;
import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationService {

    private final VerificationAdaptor verificationAdaptor;
    private final VerificationValidator verificationValidator;
    private final VisionService visionService;
    private final PassAdaptor passAdaptor;
    private final TodoRepository todoRepository;
    private final LabelRepository labelRepository;
    private final PassValidator passValidator;
    private final VerificationUsageRepository verificationUsageRepository;


    // 인증 요청 처리
    // 1.사진 인증을 합니다.
    @Transactional
    public VerificationResponse verifyTodoImage(User user, Long todoId, MultipartFile image) {

        return processVerification(
                user,
                todoId,
                VerificationType.PHOTO,     // 기존 enum에 있다면 그대로
                true,                       // PASS 사용할지 여부 (AI 인증이면 true)
                (todo) -> {                 // 여기서만 Vision + 분석 수행

                    // vision api 호출
                    List<EntityAnnotation> textAnnotations;
                    List<EntityAnnotation> labelAnnotations;

                    try {
                        textAnnotations = visionService.detectText(image);
                        labelAnnotations = visionService.detectLabels(image);
                    } catch (Exception e) {
                        throw new RuntimeException("Vision API 호출 실패", e);
                    }

                    double confidence = calculateConfidence(todo, textAnnotations, labelAnnotations);
                    boolean success = confidence >= 0.5;

                    return new VerificationResult(success, confidence);
                }
        );
    }

    // 2. 음성 인증을 합니다.
    // 음성 -> 텍스트 변환을 외부에서
    // 이를 script로 받음
    @Transactional
    public VerificationResponse verifyTodoVoice(User user, Long todoId, String transcript) {

        return processVerification(
                user,
                todoId,
                VerificationType.AUDIO,
                true,       // AI 인증 → PASS 사용
                (todo) -> {
                    // transcript 를 Vision 의 텍스트 결과처럼 하나의 EntityAnnotation 으로 감싸서 재사용
                    EntityAnnotation fakeText = EntityAnnotation.newBuilder()
                            .setDescription(transcript == null ? "" : transcript)
                            .build();

                    double confidence = calculateConfidence(
                            todo,
                            List.of(fakeText),   // 텍스트만 사용
                            List.of()            // 라벨은 비워둠
                    );

                    boolean success = confidence >= 0.5;
                    return new VerificationResult(success, confidence);
                }
        );
    }

    // 3. 위치 인증 대신 일단은 텍스트 인증으로 하겠슴.
    @Transactional
    public VerificationResponse verifyTodoText(User user, Long todoId, String userText) {

        return processVerification(
                user,
                todoId,
                VerificationType.TEXT,
                true,       // 이것도 AI 인증으로 취급 (유사도 계산)
                (todo) -> {
                    EntityAnnotation fakeText = EntityAnnotation.newBuilder()
                            .setDescription(userText == null ? "" : userText)
                            .build();

                    double confidence = calculateConfidence(
                            todo,
                            List.of(fakeText),
                            List.of()
                    );

                    boolean success = confidence >= 0.5;
                    return new VerificationResult(success, confidence);
                }
        );
    }

    // 공통 처리 메서드
    private VerificationResponse processVerification(
            User user,
            Long todoId,
            VerificationType type,
            boolean usePass,
            Function<Todo, VerificationResult> verificationLogic
    ) {
        // 1) Todo 조회
        Todo todo = todoRepository.findByTodoId(todoId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TODO_NOT_FOUND));

        // 2) 인증 가능한 투두인지 검증
        verificationValidator.validVerification(user, todo);

        // 3) 실제 인증 로직 수행
        VerificationResult result = verificationLogic.apply(todo);
        boolean success = result.isSuccess();
        double confidence = result.getConfidence();

        // 4) Verification 엔티티 저장
        Verification verification = Verification.record(type, todo.getTodoId(), success, confidence);

        verificationAdaptor.save(verification);

        // 5) PASS 차감 + 사용량 집계 (성공했을 때만)
        if (usePass && success) {
            Pass pass = passAdaptor.findByUserId(user.getId())
                    .stream()
                    .filter(p -> p.remainingCount() > 0)
                    .min((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                    .orElseThrow(() -> new GeneralException(ErrorStatus.PASS_NOT_AVAILABLE));

            pass.consume(user.getId(), passValidator);

            // 인증 사용량 기록
            increaseUsage(user.getId());
        }

        // 6) Todo 완료 처리
        if (success) {
            todo.updateComplete(true);
        }

        log.info("인증 결과 | member={}, todo={}, type={}, success={}, confidence={}",
                user.getId(), todo.getTodoId(), type, success, confidence);

        // 7) Response 생성
        return VerificationResponse.from(verification, todo, success, confidence);
    }

    // 인증 사용량 +1
    private void increaseUsage(Long userId) {
        VerificationUsage usage = verificationUsageRepository.findByUserId(userId)
                .orElseGet(() -> VerificationUsage.init(userId));

        usage.increase();
        verificationUsageRepository.save(usage);
    }



    /**
     * 결과 분석 로직
     * 카테고리 내용 기반으로 텍스트/라벨 매칭 확률 계산
     */

    private double calculateConfidence(Todo todo, List<EntityAnnotation> texts, List<EntityAnnotation> labels) {
        double score = 0.0;

        LabelType label = labelRepository.findById(todo.getLabelId())
                .orElseThrow()
                .getLabelName();

        String category = label.toString().toLowerCase();
        String content = todo.getContent().toLowerCase();

        // 텍스트 매칭 점수 계산
        for (EntityAnnotation text : texts) {
            String detected = text.getDescription().toLowerCase();

            // 내용 일부 포함 시 +0.3
            if (detected.contains(content)) {
                score += 0.3;
            }

            // 카테고리 키워드 포함 시 +0.4
            if (detected.contains(category)) {
                score += 0.4;
            }
        }

        // 라벨 분석
        for (EntityAnnotation labelText : labels) {
            String labelDescription = labelText.getDescription().toLowerCase();

            if (labelDescription.contains(category)) {
                score += 0.4;
            }

            if (content.contains(labelDescription)) {
                score += 0.3;
            }
        }

        // 최대 1.0으로 제한
        return Math.min(score, 1.0);
    }


    // vision api 결과 해석 로직 부분
    // 일단 안쓰고 있음..
    private boolean analyzeResult(List<EntityAnnotation> textAnnotations,
                                  List<EntityAnnotation> labelAnnotations,
                                  Todo todo) {
        // Todo 카테고리와 연관도가 존재하면 인증 성공
        String todoCategory = todo.getContent();

        return true;


    }
}

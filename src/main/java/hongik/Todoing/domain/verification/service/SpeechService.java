package hongik.Todoing.domain.verification.service;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import hongik.Todoing.infrastructure.aop.AuditingAi.AuditingAI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class SpeechService {
    // 음성을 텍스트로 변환하는 서비스 (STT)
    @AuditingAI("음성을 통한 텍스트 변환 호출기")
    public String stt(MultipartFile audioFile) throws IOException {

        // 1. 파일을 ByteString으로 변환
        ByteString audioBytes = ByteString.readFrom(audioFile.getInputStream());

        // 2. Google Speech API용 Audio 객체 생성
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();

        // 3. 설정 (한국어 예시: ko-KR)
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // 파일 인코딩에 맞게 조정 필요
                .setLanguageCode("ko-KR")
                .build();

        // 4. 클라이언트 생성 후 요청
        try (SpeechClient speechClient = SpeechClient.create()) {
            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setConfig(config)
                    .setAudio(audio)
                    .build();

            RecognizeResponse response = speechClient.recognize(request);

            StringBuilder transcriptBuilder = new StringBuilder();

            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcriptBuilder.append(alternative.getTranscript()).append(" ");
            }

            String transcript = transcriptBuilder.toString().trim();
            log.info("STT transcript = {}", transcript);
            return transcript;
        }
    }
}

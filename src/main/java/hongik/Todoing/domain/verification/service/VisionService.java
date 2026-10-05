package hongik.Todoing.domain.verification.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import hongik.Todoing.infrastructure.aop.AuditingAi.AuditingAI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VisionService {

    // 사진을 통한 글자 인증 api 호출기
    @AuditingAI("사진을 통한 글자 인증 호출기")
    public List<EntityAnnotation> detectText(MultipartFile image) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        // Convert MultipartFile to ByteString
        ByteString imgBytes = ByteString.readFrom(image.getInputStream());

        // Vision API 요청 생성
        Image img = Image.newBuilder()
                .setContent(imgBytes)
                .build();

        Feature feat = Feature.newBuilder()
                .setType(Feature.Type.TEXT_DETECTION)
                .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        requests.add(request);

        // Vision API 호출
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()){
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if(res.hasError()) {
                    log.info("Error: {}", res.getError().getMessage());
                    // 에러가 발생한 경우 빈 리스트 반환
                    return List.of();
                }

                return res.getTextAnnotationsList();
            }
            return List.of();
        }
    }

    // 사진을 통한 객체 인식 - label 탐색기
    @AuditingAI("사진을 통한 객체 인식 - label 탐색기")
    public List<EntityAnnotation> detectLabels(MultipartFile file) throws IOException {
        ByteString imgBytes = ByteString.readFrom(file.getInputStream());

        Image img = Image.newBuilder()
                .setContent(imgBytes).build();

        Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()){
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            AnnotateImageResponse res = response.getResponses(0);

            if(res.hasError()) {
                throw new RuntimeException("Error: " + res.getError().getMessage());
            }

            return res.getLabelAnnotationsList();

        }
    }
}

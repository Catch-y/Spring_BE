package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GPTCourseService {

    private final WebClient gptWebClient;
    private final WebClient dalleWebClient;
    private final AmazonS3Manager amazonS3Manager;

    @Value("${openai.model}")
    private String openAiModel;

    public CompletableFuture<String> generateAndUploadCourseImageAsync(String courseName, String courseDescription) {
        return generateCourseImageAsync(courseName, courseDescription)
                .thenApplyAsync(this::uploadImageToS3);
    }

    private String uploadImageToS3(String imageUrl) {
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            String keyName = "course-images/" + UUID.randomUUID() + ".png";
            long contentLength = inputStream.available();
            String contentType = "image/png";

            amazonS3Manager.uploadInputStream(keyName, inputStream, contentType, contentLength);

            return amazonS3Manager.getFileUrl(keyName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to S3", e);
        }
    }

    // 비동기 OpenAI 텍스트 API 호출
    public CompletableFuture<String> callOpenAiApiAsync(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 500,
                "temperature", 0.7
        );

        return gptWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    // 비동기로 텍스트 생성
    public CompletableFuture<String> generateCourseImageAsync(String courseName, String courseDescription) {
        String prompt = String.format(
                "Create a visually appealing image for the course titled '%s' with the theme: '%s'.",
                courseName, courseDescription
        );

        Map<String, Object> requestBody = Map.of(
                "prompt", prompt,
                "n", 1,
                "size", "256x256"
        );

        return dalleWebClient.post()
                .uri("/images/generations")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> {
                    System.err.println("Error during DALL-E API call: " + e.getMessage());
                })
                .map(this::extractImageUrl)
                .toFuture();
    }

    // DALL-E 응답에서 URL 추출
    private String extractImageUrl(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode imageUrlNode = rootNode.path("data").get(0).path("url");

            if (imageUrlNode.isMissingNode()) {
                throw new RuntimeException("Image URL not found in DALL-E response");
            }

            return imageUrlNode.asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse DALL-E response", e);
        }
    }
}
package umc.catchy.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTCourseService {

    private final WebClient gptWebClient;
    private final WebClient dalleWebClient;
    private final AmazonS3Manager amazonS3Manager;
    private static final Map<String, CompletableFuture<String>> imageRequestCache = new ConcurrentHashMap<>();

    @Value("${openai.model}")
    private String openAiModel;

    public CompletableFuture<String> generateAndUploadCourseImageAsync(String courseName, String courseDescription) {
        String cacheKey = courseName + ":" + courseDescription;
        return imageRequestCache.computeIfAbsent(cacheKey, key ->
                generateCourseImageAsync(courseName, courseDescription)
                        .thenApplyAsync(this::uploadImageToS3)
                        .whenComplete((res, ex) -> imageRequestCache.remove(cacheKey))
        );
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
                "Generate a high-quality, visually appealing image representing the course '%s'. " +
                        "Emphasize a vibrant and inviting atmosphere with scenic cafes, lively city streets, and traditional Korean aesthetics. " +
                        "Ensure a warm color palette and an immersive visual experience. Theme: '%s'.",
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
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10))  // 최대 3회 재시도, 10초 간격
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))  // 429 상태일 때만 재시도
                .doOnError(e -> log.error("Error during DALL-E API call: {}", e.getMessage()))
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
package umc.catchy.infra.openai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient gptWebClient;

    @Value("${openai.model}")
    private String model;

    public CompletableFuture<String> chat(String prompt, int maxTokens, double temperature) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", maxTokens,
                "temperature", temperature
        );

        return gptWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }
}

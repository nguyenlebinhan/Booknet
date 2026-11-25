package grp3.java.booknet.service.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/** Calls Google Gemini over REST. The key stays on the server and is never logged. */
@Component
public class GeminiClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    /** A few calls at a time, so slow answers cannot hold every request thread and DB connection. */
    private static final int MAX_CONCURRENT = 4;
    private static final Map<String, Object> ANSWER_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "onTopic", Map.of("type", "boolean"),
                    "answer", Map.of("type", "string"),
                    "bookIds", Map.of("type", "array", "items", Map.of("type", "integer"))),
            "required", List.of("onTopic", "answer", "bookIds"));

    public record Answer(boolean onTopic, String text, List<Long> bookIds) {
    }

    private final JsonMapper json;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Semaphore slots = new Semaphore(MAX_CONCURRENT);

    public GeminiClient(JsonMapper json,
                        @Value("${app.assistant.gemini.api-key:}") String apiKey,
                        @Value("${app.assistant.gemini.model:gemini-3.5-flash-lite}") String model,
                        @Value("${app.assistant.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.json = json;
        this.apiKey = apiKey.strip();
        this.model = model.strip();
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** @throws AssistantUnavailableException when no usable answer came back */
    public Answer ask(String instructions, List<AssistantTurn> history, String question) {
        if (!isConfigured()) {
            throw new AssistantUnavailableException("No Gemini API key");
        }
        if (!slots.tryAcquire()) {
            throw new AssistantUnavailableException("Too many questions at once");
        }
        try {
            HttpResponse<String> response = http.send(request(instructions, history, question),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Gemini answered with HTTP {}", response.statusCode());
                throw new AssistantUnavailableException("HTTP " + response.statusCode());
            }
            return read(response.body());
        } catch (IOException exception) {
            log.warn("Gemini request failed: {}", exception.getClass().getSimpleName());
            throw new AssistantUnavailableException("Request failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssistantUnavailableException("Interrupted");
        } finally {
            slots.release();
        }
    }

    private HttpRequest request(String instructions, List<AssistantTurn> history, String question) {
        List<Object> contents = new ArrayList<>();
        for (AssistantTurn turn : history) {
            contents.add(content("user", turn.question()));
            contents.add(content("model", json.writeValueAsString(Map.of(
                    "onTopic", true, "answer", turn.answer(), "bookIds", turn.bookIds()))));
        }
        contents.add(content("user", question));
        String body = json.writeValueAsString(Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instructions))),
                "contents", contents,
                "generationConfig", Map.of(
                        "maxOutputTokens", 2048,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", ANSWER_SCHEMA)));
        return HttpRequest.newBuilder(URI.create(baseUrl + "/v1beta/models/" + model + ":generateContent"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static Map<String, Object> content(String role, String text) {
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    private Answer read(String body) {
        try {
            StringBuilder text = new StringBuilder();
            for (JsonNode part : json.readTree(body).path("candidates").path(0).path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false)) {
                    text.append(part.path("text").asString(""));
                }
            }
            JsonNode answer = json.readTree(text.toString());
            if (answer == null || !answer.isObject()) {
                throw new AssistantUnavailableException("No answer");
            }
            boolean onTopic = answer.path("onTopic").asBoolean(false);
            String message = answer.path("answer").asString("").strip();
            if (onTopic && message.isEmpty()) {
                throw new AssistantUnavailableException("Empty answer");
            }
            List<Long> bookIds = new ArrayList<>();
            answer.path("bookIds").forEach(id -> {
                if (id.isIntegralNumber() && id.canConvertToLong()) {
                    bookIds.add(id.asLong());
                }
            });
            return new Answer(onTopic, message, bookIds);
        } catch (JacksonException exception) {
            log.warn("Gemini answer could not be read");
            throw new AssistantUnavailableException("Unreadable answer");
        }
    }
}

package grp3.java.booknet;

import com.sun.net.httpserver.HttpServer;
import grp3.java.booknet.service.assistant.AssistantTurn;
import grp3.java.booknet.service.assistant.AssistantUnavailableException;
import grp3.java.booknet.service.assistant.GeminiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The Gemini request and response format, against a local stand-in server. Does not prove live connectivity. */
class GeminiClientTests {
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;
    private int status = 200;
    private String reply;
    private String path;
    private String key;
    private String sent;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path = exchange.getRequestURI().toString();
            key = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            sent = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsRulesHistoryAndQuestionAndReadsTheAnswer() {
        reply = gemini("{\"onTopic\":true,\"answer\":\"Thử Mắt biếc.\",\"bookIds\":[7,\"x\",9]}");
        GeminiClient.Answer answer = client("secret-key").ask("the rules",
                List.of(new AssistantTurn("Earlier", "Earlier answer", List.of(3L), true, false)), "Gợi ý sách");
        assertThat(answer).isEqualTo(new GeminiClient.Answer(true, "Thử Mắt biếc.", List.of(7L, 9L)));
        assertThat(path).isEqualTo("/v1beta/models/test-model:generateContent");
        assertThat(key).isEqualTo("secret-key");
        JsonNode body = json.readTree(sent);
        assertThat(body.at("/systemInstruction/parts/0/text").asString()).isEqualTo("the rules");
        assertThat(body.at("/contents/0/role").asString()).isEqualTo("user");
        assertThat(body.at("/contents/1/role").asString()).isEqualTo("model");
        assertThat(body.at("/contents/1/parts/0/text").asString()).contains("Earlier answer");
        assertThat(body.at("/contents/2/parts/0/text").asString()).isEqualTo("Gợi ý sách");
        assertThat(body.at("/generationConfig/responseMimeType").asString()).isEqualTo("application/json");
        assertThat(sent).doesNotContain("secret-key");
    }

    @Test
    void everyUnusableResponseIsReportedAsUnavailable() {
        status = 429;
        reply = "{}";
        assertUnavailable();
        status = 200;
        for (String body : List.of("not json", "{}", "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}",
                gemini("plain text"), gemini("{\"onTopic\":true,\"answer\":\" \",\"bookIds\":[]}"), gemini("[1]"))) {
            reply = body;
            assertUnavailable();
        }
        server.stop(0);
        assertUnavailable();
    }

    @Test
    void withoutAKeyNothingIsSent() {
        assertThat(client(" ").isConfigured()).isFalse();
        assertThatThrownBy(() -> client(" ").ask("rules", List.of(), "Hi")).isInstanceOf(AssistantUnavailableException.class);
        assertThat(sent).isNull();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> client("k").ask("rules", List.of(), "Hi")).isInstanceOf(AssistantUnavailableException.class);
    }

    private GeminiClient client(String apiKey) {
        return new GeminiClient(json, apiKey, "test-model", "http://127.0.0.1:" + server.getAddress().getPort());
    }

    private String gemini(String text) {
        return json.writeValueAsString(Map.of("candidates", List.of(Map.of("content",
                Map.of("role", "model", "parts", List.of(Map.of("text", "thinking", "thought", true), Map.of("text", text)))))));
    }
}

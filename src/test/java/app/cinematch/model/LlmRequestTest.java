package app.cinematch.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests BDD pour LlmRequest. */
public class LlmRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void givenModelMessagesStreamTrue_whenCreate_thenAllFieldsMatch() {
        // Given
        String model = "qwen2.5";
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "Hello"),
                new LlmMessage("user", "How are you?")
        );

        // When constructeur principal (3 args)
        LlmRequest req = new LlmRequest(model, messages, true);

        // Then
        assertEquals(model, req.model());
        assertEquals(messages, req.messages());
        assertTrue(req.stream());
    }

    @Test
    void givenModelMessages_whenCreateWithSecondaryCtor_thenStreamIsFalse() {
        // Given
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Ping"));

        // When constructeur secondaire (2 args)
        LlmRequest req = new LlmRequest("qwen2.5", messages);

        // Then
        assertEquals("qwen2.5", req.model());
        assertEquals(messages, req.messages());
        assertFalse(req.stream());
    }

    @Test
    void givenUnknownJsonFields_whenDeserialize_thenIgnoredByAnnotation() throws Exception {
        // Given JSON avec champs inconnus
        String json = """
            {
              "model":"qwen2.5",
              "messages":[{"role":"user","content":"Yo"}],
              "stream":true,
              "extra":"ignored"
            }
        """;

        // When
        LlmRequest req = MAPPER.readValue(json, LlmRequest.class);

        // Then
        assertEquals("qwen2.5", req.model());
        assertEquals(1, req.messages().size());
        assertTrue(req.stream());
    }
}

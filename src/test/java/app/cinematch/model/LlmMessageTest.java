package app.cinematch.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests BDD pour LlmMessage. */
public class LlmMessageTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void givenRoleAndContent_whenCreateLlmMessage_thenFieldsAreAccessible() {
        // Given
        String role = "user";
        String content = "Hello";

        // When
        LlmMessage msg = new LlmMessage(role, content);

        // Then
        assertEquals(role, msg.role());
        assertEquals(content, msg.content());
    }

    @Test
    void givenUnknownJsonFields_whenDeserialize_thenIgnoredByAnnotation() throws Exception {
        // Given  JSON avec champs inconnus (grâce à @JsonIgnoreProperties)
        String json = """
            {"role":"assistant","content":"Salut","extra":"ignored"}
        """;

        // When  désérialisation
        LlmMessage msg = MAPPER.readValue(json, LlmMessage.class);

        // Then      l’objet est lu, le champ inconnu est ignoré
        assertEquals("assistant", msg.role());
        assertEquals("Salut", msg.content());
    }
}

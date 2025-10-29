package app.cinematch.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests BDD pour LlmResponse. */
public class LlmResponseTest {

    @Test
    void givenMessage_whenCreateLlmResponse_thenMessageIsStored() {
        // Given
        LlmMessage message = new LlmMessage("assistant", "Salut");

        // When
        LlmResponse response = new LlmResponse(message);

        // Then
        assertEquals(message, response.message());
    }

    @Test
    void givenNullMessage_whenCreateLlmResponse_thenAllowed() {
        // Given / When
        LlmResponse response = new LlmResponse(null);

        // Then
        assertNull(response.message());
    }
}

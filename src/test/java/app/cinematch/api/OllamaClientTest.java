package app.cinematch.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OllamaClientTest {

    private HttpClient httpMock;

    @BeforeEach
    void setUp() {
        // Given: un HttpClient mock pour éviter tout appel réseau
        httpMock = mock(HttpClient.class);
    }

    @Test
    void chat_returnsContent_whenResponseHasMessage() throws Exception {
        // Given: client avec baseUrl ayant un slash final (doit être trimé)
        OllamaClient client = new OllamaClient("http://localhost:11434/", "test-model");
        injectMock(client, "http", httpMock);

        // ... et une réponse HTTP avec un message non nul
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        String body = "{\"message\":{\"role\":\"assistant\",\"content\":\"Bonjour !\"}}";
        org.mockito.Mockito.when(response.body()).thenReturn(body);
        org.mockito.Mockito.when(httpMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        // When: on appelle chat
        String result = client.chat("sys", "usr");

        // Then: on récupère le contenu
        assertEquals("Bonjour !", result);

        // And: l’URL appelée est correcte (pas de double '/')
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpMock).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = captor.getValue();
        assertEquals(URI.create("http://localhost:11434/api/chat"), sent.uri());
    }

    @Test
    void chat_returnsVide_whenMessageIsNull() throws Exception {
        // Given: client et réponse avec "message": null
        OllamaClient client = new OllamaClient("http://host:1234", "m");
        injectMock(client, "http", httpMock);

        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        org.mockito.Mockito.when(response.body()).thenReturn("{\"message\":null}");
        org.mockito.Mockito.when(httpMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        // When
        String result = client.chat("sys", "usr");

        // Then
        assertEquals("[vide]", result);
    }

    @Test
    void chat_returnsErrorPrefix_whenHttpThrows() throws Exception {
        // Given: client et HttpClient qui jette une exception
        OllamaClient client = new OllamaClient("http://host:8080", "m");
        injectMock(client, "http", httpMock);

        org.mockito.Mockito.when(httpMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("boom"));

        // When
        String result = client.chat("sys", "usr");

        // Then: on renvoie le format d’erreur attendu
        assertTrue(result.startsWith("[Erreur Ollama] "));
        assertTrue(result.contains("boom"));
    }

    // --- utilitaire d’injection du mock dans le champ privé 'http'
    private static void injectMock(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}

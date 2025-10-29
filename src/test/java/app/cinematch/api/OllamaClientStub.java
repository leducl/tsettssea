package app.cinematch.api;

import java.util.ArrayDeque;

/** Test double simple pour simuler des réponses d'IA. */
public class OllamaClientStub {

    // File de réponses simulées pour les appels chat(system,user)
    public static final ArrayDeque<String> RESPONSES = new ArrayDeque<>();

    public OllamaClientStub(String baseUrl, String model) {
        // no-op pour les tests
    }

    public String chat(String system, String user) {
        return RESPONSES.isEmpty() ? "" : RESPONSES.removeFirst();
    }

    /** Utilitaire pour réinitialiser entre tests. */
    public static void reset() {
        RESPONSES.clear();
    }
}

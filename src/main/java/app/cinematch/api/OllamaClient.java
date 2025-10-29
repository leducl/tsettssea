package app.cinematch.api;

import app.cinematch.model.LlmMessage;
import app.cinematch.model.LlmRequest;
import app.cinematch.model.LlmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Client HTTP permettant de communiquer avec une instance locale ou distante
 * d’<b>Ollama</b> (modèle de langage) via son API REST.
 *
 * <p>Cette classe gère la sérialisation JSON des requêtes et réponses
 * à l’aide de Jackson, et envoie les messages au modèle spécifié.
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * OllamaClient client = new OllamaClient("http://localhost:11434", "mistral");
 * String reponse = client.chat("Tu es un assistant.", "Bonjour !");
 * }</pre>
 */
public class OllamaClient {

    /** URL de base du serveur Ollama, sans le slash final. */
    private final String baseUrl;

    /** Nom du modèle à utiliser pour la génération de texte. */
    private final String model;

    /** Client HTTP réutilisable pour l’envoi des requêtes. */
    private final HttpClient http = HttpClient.newHttpClient();

    /** Mapper JSON utilisé pour la (dé)sérialisation. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Crée un nouveau client Ollama configuré pour un modèle et une URL donnés.
     *
     * @param baseUrl l’adresse de l’API Ollama (ex : {@code http://localhost:11434})
     * @param model le nom du modèle à interroger (ex : {@code mistral}, {@code llama3})
     */
    public OllamaClient(String baseUrl, String model) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.model = model;
    }

    /**
     * Envoie un message au modèle de langage configuré et renvoie la réponse générée.
     *
     * <p>Cette méthode crée une requête JSON contenant un message "system"
     * et un message "user", puis interroge le point d’entrée {@code /api/chat}
     * de l’API Ollama. La réponse est convertie en {@link LlmResponse}.
     *
     * @param system le message de configuration du comportement du modèle (rôle system)
     * @param user le message utilisateur auquel le modèle doit répondre
     * @return le texte généré par le modèle, ou une chaîne d’erreur si un problème survient
     */
    public String chat(String system, String user) {
        try {
            var req = new LlmRequest(model, java.util.List.of(
                    new LlmMessage("system", system),
                    new LlmMessage("user", user)
            ));
            String json = mapper.writeValueAsString(req);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            LlmResponse resp = mapper.readValue(res.body(), LlmResponse.class);
            return resp.message() != null ? resp.message().content() : "[vide]";
        } catch (Exception e) {
            return "[Erreur Ollama] " + e.getMessage();
        }
    }
}

package app.cinematch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Représente une requête envoyée à un modèle de langage (LLM) comme Ollama.
 *
 * <p>Une requête contient :
 * <ul>
 *   <li><b>model</b> — le nom du modèle utilisé (ex. : {@code "mistral"}, {@code "llama3"})</li>
 *   <li><b>messages</b> — la liste des messages constituant le contexte de la conversation</li>
 *   <li><b>stream</b> — indique si la réponse doit être envoyée en flux continu (streaming)</li>
 * </ul>
 *
 * <p>L’annotation {@link JsonIgnoreProperties} avec {@code ignoreUnknown = true}
 * permet d’ignorer les champs non reconnus lors de la désérialisation JSON,
 * rendant cette classe plus tolérante aux changements d’API.</p>
 *
 * <p>Cette implémentation assure une <b>copie défensive</b> de la liste de messages,
 * évitant ainsi toute fuite ou modification externe de l’état interne (conforme à SpotBugs EI_EXPOSE_REP).</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * List<LlmMessage> msgs = List.of(
 *     new LlmMessage("system", "Tu es un assistant."),
 *     new LlmMessage("user", "Recommande-moi un film français.")
 * );
 * LlmRequest req = new LlmRequest("mistral", msgs);
 * }</pre>
 *
 * @param model    le nom du modèle LLM à interroger
 * @param messages la liste des messages constituant le contexte de la requête
 * @param stream   {@code true} si la réponse doit être transmise en streaming, {@code false} sinon
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmRequest(String model, List<LlmMessage> messages, boolean stream) {

    /**
     * Constructeur canonique avec copie défensive pour garantir l’immuabilité.
     *
     * @param model    le nom du modèle à utiliser
     * @param messages la liste des messages d’entrée (copiée de manière immuable)
     * @param stream   indicateur d’utilisation du mode streaming
     */
    public LlmRequest(String model, List<LlmMessage> messages, boolean stream) {
        this.model = model;
        this.messages = List.copyOf(messages != null ? messages : List.of());
        this.stream = stream;
    }

    /**
     * Constructeur simplifié sans paramètre de streaming.
     * Par défaut, le streaming est désactivé ({@code false}).
     *
     * @param model    le nom du modèle à utiliser
     * @param messages la liste des messages d’entrée
     */
    public LlmRequest(String model, List<LlmMessage> messages) {
        this(model, messages, false);
    }

    /**
     * Retourne la liste des messages de la requête sous forme immuable.
     *
     * <p>Une nouvelle copie immuable est renvoyée afin d’éviter toute
     * modification externe du contenu interne du record.</p>
     *
     * @return une liste immuable de messages {@link LlmMessage}
     */
    @Override
    public List<LlmMessage> messages() {
        return List.copyOf(this.messages);
    }
}

package app.cinematch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Représente un message échangé avec un modèle de langage (LLM),
 * tel qu’Ollama, sous la forme d’un objet simple compatible JSON.
 *
 * <p>Chaque message possède :
 * <ul>
 *   <li><b>role</b> — le rôle de l’expéditeur du message
 *   (exemples : {@code "system"}, {@code "user"}, {@code "assistant"})</li>
 *   <li><b>content</b> — le texte ou le contenu du message</li>
 * </ul>
 *
 * <p>L’annotation {@link JsonIgnoreProperties} avec {@code ignoreUnknown = true}
 * permet d’ignorer les champs inconnus lors de la désérialisation JSON,
 * garantissant ainsi la robustesse face à d’éventuelles évolutions du schéma.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * LlmMessage systemMsg = new LlmMessage("system", "Tu es un assistant utile.");
 * LlmMessage userMsg = new LlmMessage("user", "Donne-moi un film à regarder.");
 * }</pre>
 *
 * @param role    le rôle de l’expéditeur du message (system, user, assistant…)
 * @param content le contenu textuel du message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmMessage(String role, String content) { }

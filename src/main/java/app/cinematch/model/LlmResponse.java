package app.cinematch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Représente la réponse renvoyée par un modèle de langage (LLM),
 * généralement à la suite d’une requête {@link LlmRequest}.
 *
 * <p>Une réponse contient un seul message, correspondant à la sortie
 * du modèle de langage. Ce message inclut le rôle (souvent {@code "assistant"})
 * et le contenu textuel généré.</p>
 *
 * <p>L’annotation {@link JsonIgnoreProperties} avec {@code ignoreUnknown = true}
 * permet d’ignorer les champs inconnus lors de la désérialisation JSON,
 * afin d’assurer la compatibilité avec différentes versions d’API Ollama.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * LlmResponse resp = new LlmResponse(new LlmMessage("assistant", "Voici un film à voir !"));
 * System.out.println(resp.message().content());
 * }</pre>
 *
 * @param message le message généré par le modèle (rôle et contenu)
 * @see LlmMessage
 * @see LlmRequest
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmResponse(LlmMessage message) { }

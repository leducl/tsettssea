package app.cinematch.agent;

import java.util.LinkedList;

/**
 * Mémoire courte de conversation : conserve les derniers échanges
 * pour permettre à l'IA de répondre de manière contextuelle.
 */
public class ConversationMemory {

    private final LinkedList<Message> history = new LinkedList<>();
    private final int maxMessages;

    public ConversationMemory(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public void addUserMessage(String content) {
        addMessage("Utilisateur", content);
    }

    public void addAssistantMessage(String content) {
        addMessage("IA", content);
    }

    private void addMessage(String role, String content) {
        history.add(new Message(role, content));
        if (history.size() > maxMessages) {
            history.removeFirst(); // Supprime le plus ancien
        }
    }

    /**
     * Retourne l'historique sous forme de texte lisible par le modèle.
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        for (Message msg : history) {
            sb.append(msg.role()).append(" : ").append(msg.content()).append("\n");
        }
        return sb.toString();
    }

    public record Message(String role, String content) {}
}

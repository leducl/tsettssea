package app.cinematch.agent;

import app.cinematch.api.OllamaClient;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ChatAgent {

    private final OllamaClient ollama;
    private Profile profile;
    private final Memory memory;
    private final ConversationMemory convMemory;

    /** Délégué optionnel (ex. LangChain4jAgentBridge::ask). */
    private final Function<String, String> askDelegate;

    /** Constructeur historique (sans délégué) */
    public ChatAgent(final OllamaClient ollama, final Profile profile, final Memory ignored) {
        this(ollama, profile, ignored, null); // ✅ initialise askDelegate à null
    }

    /** Nouveau constructeur avec délégué */
    public ChatAgent(final OllamaClient ollama,
                     final Profile profile,
                     final Memory ignored,
                     final Function<String, String> askDelegate) {
        this.ollama = Objects.requireNonNull(ollama);
        this.profile = Objects.requireNonNull(profile);
        // Memory interne stateless pour éviter l'exposition externe
        this.memory = new Memory();
        this.convMemory = new ConversationMemory(6);
        this.askDelegate = askDelegate; // ✅ assignation du champ final
    }

    public void setProfile(final Profile profile) { this.profile = Objects.requireNonNull(profile); }

    public String ask(final String userPrompt) {
        // Si un délégué (LangChain4j) est fourni, on lui confie la réponse
        if (askDelegate != null) {
            convMemory.addUserMessage(userPrompt);
            final String response = askDelegate.apply(userPrompt);
            convMemory.addAssistantMessage(response);
            return response;
        }

        // ----- Flux "classique" (OllamaClient maison) -----
        convMemory.addUserMessage(userPrompt);

        final List<String> seen = memory.seen();
        final List<String> wishlist = memory.toWatch();
        final List<String> disliked = memory.notInterested();

        final String seenStr = seen.isEmpty() ? "aucun film enregistré" : String.join(", ", seen);
        final String wishStr = wishlist.isEmpty() ? "aucun film enregistré" : String.join(", ", wishlist);
        final String badStr  = disliked.isEmpty() ? "aucun film enregistré" : String.join(", ", disliked);

        final String ls = System.lineSeparator();
        final StringBuilder sb = new StringBuilder(512);
        sb.append("Tu es un expert du cinéma francophone, spécialiste des recommandations personnalisées.").append(ls)
                .append("Tes réponses doivent toujours être en français, avec un ton naturel, amical et professionnel.").append(ls).append(ls)
                .append("Voici les informations sur les goûts de l’utilisateur :").append(ls)
                .append("- Films déjà vus : ").append(seenStr).append(ls)
                .append("- Films qu’il souhaite voir : ").append(wishStr).append(ls)
                .append("- Films qu’il n’aime pas : ").append(badStr).append(ls).append(ls)
                .append("Contexte récent de la conversation :").append(ls)
                .append(convMemory.toPromptString()).append(ls).append(ls)
                .append("Rappelle-toi :").append(ls)
                .append("- Ne repropose jamais un film déjà vu ou non souhaité.").append(ls)
                .append("- Inspire-toi du contexte précédent pour rester cohérent.").append(ls)
                .append("- Réponds de façon fluide, ≤ 100 mots, sans répétition.").append(ls);

        final String response = ollama.chat(sb.toString(), userPrompt);
        convMemory.addAssistantMessage(response);
        return response;
    }

    public Memory getMemory() { return new Memory(); }
    public Profile getProfile() { return profile; }
}

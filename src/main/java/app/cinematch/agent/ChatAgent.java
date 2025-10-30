package app.cinematch.agent;

import app.cinematch.api.OllamaClient;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Représente un agent conversationnel connecté à un modèle de langage Ollama,
 * capable de générer des recommandations de films personnalisées en fonction
 * des préférences de l’utilisateur.
 *
 * <p>L’agent maintient un profil utilisateur et une mémoire interne (stateless)
 * permettant de conserver la trace des films vus, à voir ou non appréciés.
 * Il formate dynamiquement le prompt système envoyé au modèle Ollama pour
 * obtenir des réponses cohérentes et pertinentes.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * OllamaClient ollama = new OllamaClient("http://localhost:11434", "mistral");
 * Profile profil = new Profile("Simon");
 * ChatAgent agent = new ChatAgent(ollama, profil, null);
 * String reponse = agent.ask("Peux-tu me recommander un film français récent ?");
 * }</pre>
 *
 * @see app.cinematch.api.OllamaClient
 * @see app.cinematch.agent.Memory
 * @see app.cinematch.agent.Profile
 */
public final class ChatAgent {

    /** Profil utilisateur contenant les préférences générales. */
    private Profile profile;

    /**
     * Mémoire interne stateless.
     * Une nouvelle instance est créée localement pour éviter toute fuite de représentation
     * (évite l’avertissement SpotBugs EI_EXPOSE_REP2).
     */
    private final Memory memory;

    /** Mémoire de conversation gérée par LangChain4j. */
    private final ChatMemory chatMemory;

    /** Assistant conversationnel construit avec LangChain4j. */
    private final CineMatchAssistant assistant;

    /** Identifiant unique de mémoire pour l’agent courant. */
    private static final String MEMORY_ID = "cinematch-session";

    /** Prompt système de base partagé par toutes les requêtes. */
    private static final String SYSTEM_PROMPT = """
            Tu es CineMatch, un expert du cinéma francophone.
            Utilise systématiquement les outils disponibles pour comprendre les goûts, l’historique et le profil conversationnel de l’utilisateur.
            Reste en français, conserve un ton chaleureux et professionnel, et réponds en au plus 100 mots.
            Ne recommande jamais de film déjà vu ou explicitement refusé.
            """;

    /**
     * Construit un nouvel agent conversationnel basé sur Ollama, avec un profil et une mémoire interne.
     *
     * @param ollama  le client Ollama à utiliser pour les échanges
     * @param profile le profil utilisateur associé à cet agent
     * @param ignored paramètre mémoire ignoré pour éviter l’exposition d’une instance externe
     */
    public ChatAgent(final OllamaClient ollama, final Profile profile, final Memory ignored) {
        this.profile = profile;
        // Instance interne propre (Memory est stateless, donc pas de perte fonctionnelle)
        this.memory = new Memory();

        String baseUrl = extractField(ollama, "baseUrl", "http://localhost:11434");
        String modelName = extractField(ollama, "model", "qwen2.5:7b-instruct");

        ChatLanguageModel chatModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(12);
        ChatMemoryProvider memoryProvider = ignoredId -> chatMemory;

        CineMatchTools tools = new CineMatchTools(this.memory, () -> this.profile);

        List<ToolSpecification> toolSpecifications = List.of(
                createNoArgTool(
                        CineMatchTools.RECUPERER_PROFIL,
                        "Fournit toutes les instructions du profil conversationnel actif."),
                createNoArgTool(
                        CineMatchTools.FILMS_DEJA_VUS,
                        "Liste les films déjà vus par l’utilisateur. Utilise cette information pour éviter les doublons."),
                createNoArgTool(
                        CineMatchTools.FILMS_A_REGARDER,
                        "Liste les films que l’utilisateur souhaite voir bientôt. Privilégie ces envies dans tes réponses."),
                createNoArgTool(
                        CineMatchTools.FILMS_REFUSES,
                        "Liste les films que l’utilisateur ne souhaite pas voir. Ne les recommande jamais.")
        );

        ToolExecutor toolExecutor = tools::execute;

        this.assistant = AiServices.builder(CineMatchAssistant.class)
                .chatLanguageModel(chatModel)
                .systemMessage(SYSTEM_PROMPT)
                .chatMemoryProvider(memoryProvider)
                .toolSpecifications(toolSpecifications)
                .toolExecutor(toolExecutor)
                .build();
    }

    /**
     * Met à jour le profil utilisateur associé à l’agent.
     *
     * @param profile le nouveau profil utilisateur à appliquer
     */
    public void setProfile(final Profile profile) {
        this.profile = profile;
    }

    /**
     * Envoie un message de l’utilisateur à l’agent et retourne la réponse générée par Ollama.
     *
     * <p>Grâce à LangChain4j, l’agent peut interroger dynamiquement les outils exposés
     * (profil actif, films vus, envies, refus) et exploiter la mémoire de conversation
     * pour générer des recommandations cohérentes d’un tour à l’autre.</p>
     *
     * @param userPrompt le message de l’utilisateur (question, demande de recommandation, etc.)
     * @return la réponse générée par le modèle de langage
     */
    public String ask(final String userPrompt) {
        return assistant.chat(MEMORY_ID, userPrompt);
    }

    /**
     * Retourne une nouvelle instance de {@link Memory}, garantissant l’absence
     * de fuite de représentation interne.
     *
     * @return une nouvelle instance de {@link Memory}
     */
    public Memory getMemory() {
        return new Memory();
    }

    /**
     * Retourne le profil utilisateur actuellement associé à l’agent.
     *
     * @return le profil de l’utilisateur
     */
    public Profile getProfile() {
        return profile;
    }

    /**
     * Extrait un champ privé de {@link OllamaClient} pour construire le modèle LangChain4j.
     *
     * @param client l’instance de client existante
     * @param fieldName le nom du champ privé à extraire
     * @param defaultValue valeur par défaut si l’extraction échoue
     * @return la valeur du champ ou {@code defaultValue} si inaccessible
     */
    private static String extractField(final OllamaClient client,
                                       final String fieldName,
                                       final String defaultValue) {
        try {
            final Field field = OllamaClient.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            final Object value = field.get(client);
            return value != null ? value.toString() : defaultValue;
        } catch (ReflectiveOperationException ex) {
            return defaultValue;
        }
    }

    /**
     * Interface décrivant l’assistant géré par LangChain4j.
     */
    private interface CineMatchAssistant {
        String chat(@MemoryId String memoryId, @UserMessage String userPrompt);
    }

    private static ToolSpecification createNoArgTool(final String name, final String description) {
        return ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(ToolParameters.builder().build())
                .build();
    }

    /**
     * Outils exposés à l’assistant pour accéder aux préférences persistantes.
     */
    private static final class CineMatchTools {

        private static final String RECUPERER_PROFIL = "recupererProfil";
        private static final String FILMS_DEJA_VUS = "filmsDejaVus";
        private static final String FILMS_A_REGARDER = "filmsARegarder";
        private static final String FILMS_REFUSES = "filmsRefuses";

        private final Memory memory;
        private final Supplier<Profile> profileSupplier;

        private CineMatchTools(final Memory memory, final Supplier<Profile> profileSupplier) {
            this.memory = Objects.requireNonNull(memory, "memory");
            this.profileSupplier = Objects.requireNonNull(profileSupplier, "profileSupplier");
        }

        private String execute(final ToolExecutionRequest request) {
            final String name = request.name();
            return switch (name) {
                case RECUPERER_PROFIL -> recupererProfil();
                case FILMS_DEJA_VUS -> filmsDejaVus();
                case FILMS_A_REGARDER -> filmsARegarder();
                case FILMS_REFUSES -> filmsRefuses();
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };
        }

        private String recupererProfil() {
            final Profile current = profileSupplier.get();
            if (current == null) {
                return "Profil inconnu : répondre en français avec un ton professionnel et concis.";
            }
            final String ls = System.lineSeparator();
            return new StringBuilder()
                    .append("Nom : ").append(current.name()).append(ls)
                    .append("Langue : ").append(current.language()).append(ls)
                    .append("Contraintes : ").append(current.constraints()).append(ls)
                    .append("Instructions : ").append(ls)
                    .append(current.systemPrompt())
                    .toString();
        }

        private String filmsDejaVus() {
            return joinList(memory.seen());
        }

        private String filmsARegarder() {
            return joinList(memory.toWatch());
        }

        private String filmsRefuses() {
            return joinList(memory.notInterested());
        }

        private static String joinList(final List<String> values) {
            return values.isEmpty() ? "aucun film enregistré" : String.join(", ", values);
        }
    }
}

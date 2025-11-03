package app.cinematch;

import app.cinematch.api.OllamaClient;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Service de recommandation de films.
 *
 * <p>Rôles principaux :</p>
 * <ul>
 *   <li>Dialogue avec {@link OllamaClient} pour générer des propositions ;</li>
 *   <li>Persistance (title, status) via un « sink » injectable
 *       ({@link JsonStorage#addOrUpdate(String, String)} par défaut).</li>
 * </ul>
 */
public class MovieRecommenderService {

    /** Parser JSON (Jackson). */
    private static final ObjectMapper PARSER = new ObjectMapper();

    /** Client Ollama sous-jacent. */
    private final OllamaClient ollama;

    /** Générateur aléatoire pour les fallbacks. */
    private final Random random = new Random();

    /** Point d'injection pour la persistance (title, status) -> void. */
    private final BiConsumer<String, String> storageSink;

    // =========================
    // CONSTRUCTEURS
    // =========================

    /**
     * Constructeur « prod » : utilise {@link JsonStorage#addOrUpdate(String, String)} par défaut.
     *
     * @param baseUrl URL de l’instance Ollama
     * @param model   nom du modèle LLM
     */
    public MovieRecommenderService(final String baseUrl, final String model) {
        this(new OllamaClient(baseUrl, model), JsonStorage::addOrUpdate);
    }

    /**
     * Constructeur « testable » : injection du sink (ex. {@code JsonStorageMock::addOrUpdate}).
     *
     * @param baseUrl     URL de l’instance Ollama
     * @param model       nom du modèle LLM
     * @param storageSink fonction de persistance (title, status)
     */
    public MovieRecommenderService(
            final String baseUrl,
            final String model,
            final BiConsumer<String, String> storageSink
    ) {
        this(new OllamaClient(baseUrl, model), storageSink);
    }

    /**
     * Constructeur avancé : injection directe du client Ollama et du sink.
     *
     * @param ollama      client Ollama
     * @param storageSink fonction de persistance (title, status)
     */
    public MovieRecommenderService(
            final OllamaClient ollama,
            final BiConsumer<String, String> storageSink
    ) {
        this.ollama = ollama;
        this.storageSink = (storageSink != null) ? storageSink : JsonStorage::addOrUpdate;
    }

    // =========================
    // API PUBLIQUE
    // =========================

    /**
     * Recommande un film à partir d’un titre apprécié.
     *
     * @param likedTitle film apprécié (point de départ)
     * @return recommandation enrichie (pitch mentionnant le film d’origine)
     */
    public Recommendation recommendFromLike(final String likedTitle) {
        final String system =
                "Tu es un assistant cinéma ultra créatif. Tu connais les films existants et tu peux aussi "
                        + "N’inclus jamais : jeux vidéo, livres ou autres qui ne sont pas des films, séries ou documentaires. "
                        + "imaginer un faux service de streaming crédible. Réponds toujours en JSON strict, "
                        + "sans texte supplémentaire.";
        final String user =
                "Film apprécié : '" + likedTitle + "'. Propose une recommandation nuancée avec ce format "
                        + "Propose EXCLUSIVEMENT un film, série ou documentaire similaire. "
                        + "JSON : {\"title\":\"Titre exact\",\"pitch\":\"Pourquoi ce choix\",\"year\":\""
                        + "(optionnel)\",\"platform\":\"Plateforme fictive ou réelle\"}. "
                        + "Le pitch doit faire le lien avec le film donné.";

        final Recommendation rec = requestRecommendation(system, user, "Inspiré de " + likedTitle);

        // S'assurer que le pitch mentionne le film aimé
        String reason = rec.reason();
        if (!reason.toLowerCase().contains(likedTitle.toLowerCase())) {
            reason = reason + " — Inspiré de " + likedTitle;
        }
        return new Recommendation(rec.title(), reason, rec.platform(), null);
    }

    /**
     * Recommande un film de manière aléatoire (idée « pépite »).
     *
     * @return recommandation générée
     */
    public Recommendation recommendRandom() {
        final String system =
                "Tu es un programmateur de ciné-club. Suggère un film ou une pépite à découvrir. "
                        + "Réponds uniquement avec un JSON strict.";
        final String user =
                "Génère une idée de film à regarder avec ce format : {\"title\":\"...\",\"pitch\":\"...\","
                        + "\"year\":\"(optionnel)\",\"platform\":\"Plateforme fictive ou réelle\"}. "
                        + "Le pitch doit donner envie.";
        return requestRecommendation(system, user, "Suggestion IA");
    }

    /**
     * Génère une courte description (2–3 phrases) sans spoiler.
     *
     * @param movieTitle titre du film
     * @return description produite par le LLM
     */
    public String generateDescription(final String movieTitle) {
        final String system = "Tu es un critique cinéma. Donne une courte description, sans spoiler.";
        final String user =
                "Décris le film '" + movieTitle + "' en 2 à 3 phrases maximum avec un style immersif.";
        return ollama.chat(system, user);
    }

    /**
     * Marque un film avec un statut ({@code envie}, {@code deja_vu}, {@code pas_interesse}, etc.).
     *
     * @param title  titre du film
     * @param status statut à appliquer
     */
    public void mark(final String title, final String status) {
        storageSink.accept(title, status);
    }

    // =========================
    // INTERNE / UTILITAIRES
    // =========================

    /**
     * Exécute une requête de recommandation auprès du LLM, puis applique des fallbacks robustes.
     *
     * @param system        prompt système
     * @param user          prompt utilisateur
     * @param defaultReason raison par défaut si aucun pitch exploitable
     * @return recommandation normalisée
     */
    private Recommendation requestRecommendation(
            final String system,
            final String user,
            final String defaultReason
    ) {
        final String raw = ollama.chat(system, user).trim();
        final ParsedRecommendation parsed = parse(raw);

        final String title = firstNonBlank(
                parsed.title,
                extractFirstMeaningfulLine(raw),
                "Suggestion mystère"
        );

        String pitch = firstNonBlank(parsed.pitch, defaultReason);
        if (parsed.year != null && !parsed.year.isBlank()) {
            pitch = pitch + " — année suggérée : " + parsed.year.trim();
        }

        final String platform = firstNonBlank(parsed.platform, fallbackPlatform());
        return new Recommendation(title, pitch, platform, null);
    }

    /**
     * Parse le JSON (éventuel) retourné par le LLM.
     *
     * @param raw texte brut renvoyé par le LLM
     * @return structure partiellement remplie, ou vide en cas d’échec
     */
    private ParsedRecommendation parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedRecommendation();
        }
        final String json = extractJsonObject(raw);
        if (json == null) {
            return new ParsedRecommendation();
        }
        try {
            final JsonNode node = PARSER.readTree(json);
            final ParsedRecommendation parsed = new ParsedRecommendation();
            parsed.title = Optional.ofNullable(node.get("title"))
                    .map(JsonNode::asText)
                    .orElse(null);
            parsed.pitch = Optional.ofNullable(node.get("pitch"))
                    .map(JsonNode::asText)
                    .orElse(null);
            parsed.year = Optional.ofNullable(node.get("year"))
                    .map(JsonNode::asText)
                    .orElse(null);
            parsed.platform = Optional.ofNullable(node.get("platform"))
                    .map(JsonNode::asText)
                    .orElse(null);
            return parsed;
        } catch (IOException e) {
            return new ParsedRecommendation();
        }
    }

    /**
     * Extrait le plus grand objet JSON { ... } d’un texte.
     *
     * @param raw texte brut
     * @return objet JSON sous forme de chaîne ou {@code null} si introuvable
     */
    private String extractJsonObject(final String raw) {
        final int start = raw.indexOf('{');
        final int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return null;
    }

    /**
     * Retourne la première ligne « significative » d’un texte (hors puces et espaces).
     *
     * @param raw texte brut
     * @return ligne nettoyée ou chaîne vide
     */
    private String extractFirstMeaningfulLine(final String raw) {
        if (raw == null) {
            return "";
        }
        final String[] lines = raw.split("\\R");
        for (String line : lines) {
            final String cleaned = line.replaceAll("^[\t•\\-:\\s]+", "").trim();
            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        }
        return "";
    }

    /**
     * Plateforme de repli aléatoire quand le modèle n’en fournit pas.
     *
     * @return nom de plateforme plausible
     */
    private String fallbackPlatform() {
        switch (random.nextInt(4)) {
            case 0:
                return "Cinéma du Coin+";
            case 1:
                return "StreamFiction";
            case 2:
                return "Club Cinéphile";
            default:
                return "Festival Replay";
        }
    }

    /**
     * Retourne la première valeur non vide/non blanche parmi les arguments.
     *
     * @param values valeurs candidates
     * @return première valeur utile, sinon chaîne vide
     */
    private String firstNonBlank(final String... values) {
        for (String value : values) {
            if (value != null) {
                final String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    /** DTO interne pour le parsing JSON. */
    private static class ParsedRecommendation {
        String title;
        String pitch;
        String year;
        String platform;
    }
}

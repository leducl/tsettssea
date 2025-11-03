package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Outils "liste d'envie" exposés au LLM.
 * Le LLM peut les appeler pour modifier/consulter le stockage JSON.
 */
public class WishlistTools {

    @Tool("Ajoute un film à la liste d'envie (statut 'envie').")
    public String addToWishlist(@P("title") String title) {
        String cleaned = normalize(title);
        if (cleaned.isBlank()) return "ERROR:EMPTY_TITLE";

        // Fallback : si l'utilisateur a donné plusieurs titres (virgules / retours ligne)
        if (cleaned.contains(",") || cleaned.contains("\n")) {
            int n = 0;
            for (String part : cleaned.split("[,\n]")) {
                String t = normalize(part);
                if (!t.isBlank()) { JsonStorage.addOrUpdate(t, "envie"); n++; }
            }
            return "ADDED_MANY:" + n;
        }

        JsonStorage.addOrUpdate(cleaned, "envie");
        return "ADDED:" + cleaned;
    }


    @Tool("Retire un film de la liste d'envie en le marquant 'pas_interesse'.")
    public String removeFromWishlist(@P("title") String title) {
        String cleaned = normalize(title);
        if (cleaned.isBlank()) {
            return "ERROR:EMPTY_TITLE";
        }
        JsonStorage.addOrUpdate(cleaned, "pas_interesse");
        return "REMOVED:" + cleaned;
    }

    @Tool("Retourne la liste des films pour un statut donné ('envie', 'pas_interesse', 'deja_vu').")
    public List<String> getListByStatus(@P("status") String status) {
        String s = (status == null || status.isBlank())
                ? "envie"
                : status.trim().toLowerCase(Locale.ROOT);

        return JsonStorage.getByStatus(s).stream()
                .map(this::normalize)
                .filter(t -> !t.isBlank())  // évite toute entrée vide
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalize(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\"“”«»]", "").trim();
        return t.replaceAll("\\s{2,}", " ");
    }
}

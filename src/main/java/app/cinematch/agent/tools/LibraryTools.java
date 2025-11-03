package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Locale;

public class LibraryTools {

    private final MovieRecommenderService service;

    public LibraryTools(MovieRecommenderService service) {
        this.service = service;
    }

    @Tool("Marque un film comme 'deja_vu'.")
    public String markAsSeen(@P("title") String title) {
        String t = norm(title);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        JsonStorage.addOrUpdate(t, "deja_vu");
        return "SEEN:" + t;
    }

    @Tool("Marque un film comme 'pas_interesse'.")
    public String markAsDisliked(@P("title") String title) {
        String t = norm(title);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        JsonStorage.addOrUpdate(t, "pas_interesse");
        return "DISLIKED:" + t;
    }

    @Tool("Change le statut d'un film vers 'envie', 'pas_interesse' ou 'deja_vu'.")
    public String setStatus(@P("title") String title, @P("status") String status) {
        String t = norm(title);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        String s = normStatus(status);
        JsonStorage.addOrUpdate(t, s);
        return "STATUS_CHANGED:" + t + "->" + s;
    }

    @Tool("Génère une brève description (2–4 phrases) du film donné.")
    public String generateDescription(@P("title") String title) {
        String t = norm(title);
        if (t.isBlank()) return "ERROR:EMPTY_TITLE";
        return service.generateDescription(t);
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\"“”«»]", "").trim();
        return t.replaceAll("\\s{2,}", " ");
    }
    private static String normStatus(String s) {
        if (s == null) return "envie";
        String x = s.trim().toLowerCase(Locale.ROOT);
        return switch (x) {
            case "envie", "pas_interesse", "deja_vu" -> x;
            default -> "envie";
        };
    }
}

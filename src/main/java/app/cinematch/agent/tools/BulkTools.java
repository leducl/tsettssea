package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Arrays;
import java.util.Locale;

public class BulkTools {

    @Tool("Ajoute plusieurs films à la wishlist. 'titles' séparés par virgule ou saut de ligne.")
    public String addManyToWishlist(@P("titles") String titles) {
        int n = 0;
        for (String t : split(titles)) {
            String s = norm(t);
            if (!s.isBlank()) { JsonStorage.addOrUpdate(s, "envie"); n++; }
        }
        return "ADDED_MANY:" + n;
    }

    @Tool("Retire plusieurs films de la wishlist (les marque 'pas_interesse').")
    public String removeManyFromWishlist(@P("titles") String titles) {
        int n = 0;
        for (String t : split(titles)) {
            String s = norm(t);
            if (!s.isBlank()) { JsonStorage.addOrUpdate(s, "pas_interesse"); n++; }
        }
        return "REMOVED_MANY:" + n;
    }

    @Tool("Applique un statut ('envie','pas_interesse','deja_vu') à plusieurs films.")
    public String setManyStatus(@P("titles") String titles, @P("status") String status) {
        String st = normStatus(status);
        int n = 0;
        for (String t : split(titles)) {
            String s = norm(t);
            if (!s.isBlank()) { JsonStorage.addOrUpdate(s, st); n++; }
        }
        return "STATUS_MANY:" + n + "->" + st;
    }

    private static String[] split(String s) {
        if (s == null) return new String[0];
        return Arrays.stream(s.split("[,\n]"))
                .map(String::trim).toArray(String[]::new);
    }
    private static String norm(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\"“”«»]", "").trim();
        return t.replaceAll("\\s{2,}", " ");
    }
    private static String normStatus(String s) {
        if (s == null) return "envie";
        String x = s.trim().toLowerCase(Locale.ROOT);
        return switch (x) { case "envie","pas_interesse","deja_vu" -> x; default -> "envie"; };
    }
}

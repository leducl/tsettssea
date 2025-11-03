package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.*;

public class MaintenanceTools {

    @Tool("Supprime visuellement les entrées vides/quotes-only d'une liste en les marquant 'pas_interesse'.")
    public String pruneBlanksInStatus(@P("status") String status) {
        String st = normStatus(status);
        int n = 0;
        for (String t : JsonStorage.getByStatus(st)) {
            String cleaned = norm(t);
            if (cleaned.isBlank()) { JsonStorage.addOrUpdate(cleaned, "pas_interesse"); n++; }
        }
        return "PRUNED:" + n + " in " + st;
    }

    @Tool("Renomme un titre : copie le statut si trouvé, puis marque l'ancien en 'pas_interesse'.")
    public String renameTitle(@P("oldTitle") String oldTitle, @P("newTitle") String newTitle) {
        String oldT = norm(oldTitle), newT = norm(newTitle);
        if (oldT.isBlank() || newT.isBlank()) return "ERROR:EMPTY_TITLE";
        String status = findStatusIgnoreCase(oldT);
        if (status == null) status = "envie";
        JsonStorage.addOrUpdate(newT, status);
        JsonStorage.addOrUpdate(oldT, "pas_interesse");
        return "RENAMED:" + oldT + "->" + newT + " (" + status + ")";
    }

    @Tool("Retourne la liste triée pour un statut donné ('asc' ou 'desc').")
    public java.util.List<String> getListByStatusSorted(@P("status") String status, @P("order") String order) {
        String st = normStatus(status);
        List<String> list = new ArrayList<>();
        for (String t : JsonStorage.getByStatus(st)) {
            String s = norm(t);
            if (!s.isBlank()) list.add(s);
        }
        list.sort(String::compareToIgnoreCase);
        if ("desc".equalsIgnoreCase(order)) Collections.reverse(list);
        return list;
    }

    @Tool("Donne des statistiques simples (compte par statut). Utilise detail='all' par défaut.")
    public String getStats(@P("detail") String detail) {
        int envie = JsonStorage.getByStatus("envie").size();
        int nope  = JsonStorage.getByStatus("pas_interesse").size();
        int seen  = JsonStorage.getByStatus("deja_vu").size();
        int total = envie + nope + seen;
        return "STATS: total=" + total + " | envie=" + envie + " | pas_interesse=" + nope + " | deja_vu=" + seen;
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
    private static String findStatusIgnoreCase(String title) {
        for (String st : new String[]{"envie","pas_interesse","deja_vu"}) {
            for (String t : JsonStorage.getByStatus(st)) {
                if (t != null && t.equalsIgnoreCase(title)) return st;
            }
        }
        return null;
    }
}

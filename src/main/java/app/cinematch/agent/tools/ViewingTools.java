package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ViewingTools {

    private final MovieRecommenderService service;
    private final Random rnd = new Random();

    public ViewingTools(MovieRecommenderService service) {
        this.service = service;
    }

    @Tool("Propose le prochain film à regarder à partir de la wishlist. " +
            "strategy='random' ou 'first'. withDescription='true' pour inclure une courte description.")
    public String pickNextToWatch(@P("strategy") String strategy, @P("withDescription") String withDescription) {
        // copie défensive -> liste mutable
        java.util.List<String> wl = new java.util.ArrayList<>(app.cinematch.util.JsonStorage.getByStatus("envie"));

        wl.removeIf(s -> s == null || s.trim().isEmpty());
        if (wl.isEmpty()) return "NEXT:EMPTY";

        String pick;
        if ("first".equalsIgnoreCase(strategy)) {
            pick = wl.get(0);
        } else {
            pick = wl.get(new java.util.Random().nextInt(wl.size()));
        }
        pick = pick.replaceAll("[\"“”«»]", "").trim();

        if ("true".equalsIgnoreCase(withDescription)) {
            String desc = service.generateDescription(pick);
            return "NEXT:" + pick + " | " + desc;
        }
        return "NEXT:" + pick;
    }
}

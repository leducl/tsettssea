package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import dev.langchain4j.agent.tool.Tool;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Orchestrateur multi-intentions (pattern "Orchestrator/Coordinator").
 * Il parse la consigne utilisateur, produit un plan d'actions puis exécute ces actions.
 *
 * Actions supportées :
 *  - Add(title)          -> ajoute en "envie"
 *  - Remove(title)       -> marque "pas_interesse"
 *  - SetStatus(title,s)  -> s ∈ {envie, deja_vu, pas_interesse}
 *
 * Exemples :
 *   "ajoute Drive à ma liste et supprime Dune de ma liste"
 *   "marque \"Jojo Rabbit\" comme déjà vu et mets Matrix en pas_interesse"
 *   "ajoute Alien, Heat et \"Blade Runner 2049\" à ma wishlist; puis retire Parasite."
 */
public final class MultiActionTools {

    // --- Domaine statuts (on garde la compatibilité String avec JsonStorage) ---
    private static final String ENVIE = "envie";
    private static final String DEJA_VU = "deja_vu";
    private static final String PAS_INTERESSE = "pas_interesse";

    // --- Vocabulaire (FR + petits alias) ---
    private static final Set<String> ADD_VERBS = Set.of(
            "ajoute", "ajouter", "mets", "mettre", "add", "place", "placer"
    );
    private static final Set<String> REMOVE_VERBS = Set.of(
            "retire", "retirer", "supprime", "supprimer", "enleve", "enlève"
    );
    private static final Set<String> LIST_HINTS = Set.of("liste", "wishlist", "envie");
    private static final Map<String, String> STATUS_ALIASES = Map.ofEntries(
            Map.entry("envie", ENVIE),
            Map.entry("liste d'envie", ENVIE),
            Map.entry("wishlist", ENVIE),
            Map.entry("déjà vu", DEJA_VU),
            Map.entry("deja vu", DEJA_VU),
            Map.entry("déjà-vu", DEJA_VU),
            Map.entry("deja_vu", DEJA_VU),
            Map.entry("pas interessé", PAS_INTERESSE),
            Map.entry("pas intéressé", PAS_INTERESSE),
            Map.entry("pas_interesse", PAS_INTERESSE),
            Map.entry("pas interesse", PAS_INTERESSE)
    );

    private static final Pattern QUOTES = Pattern.compile("[\"“”«»]");

    // =============== 1) MODELE D’ACTIONS ===============

    /** Action abstraite (Java 17). */
    sealed interface Action permits Add, Remove, SetStatus {}

    /** Ajout du film en "envie". */
    record Add(String title) implements Action {}

    /** Marque "pas_interesse". */
    record Remove(String title) implements Action {}

    /** Fixe un statut explicite. */
    record SetStatus(String title, String status) implements Action {}

    // =============== 2) PARSEUR : Texte -> Plan d’actions ===============

    static final class Parser {

        static List<Action> parse(String instruction) {
            if (instruction == null || instruction.trim().isEmpty()) return List.of();

            String raw = instruction.trim();
            // Segmentation grossière par "et", "puis", ., ;
            String[] segments = raw.split("(?i)\\s+(et|puis)\\s+|[.;]");
            List<Action> plan = new ArrayList<>();

            for (String seg0 : segments) {
                String seg = seg0.trim();
                if (seg.isEmpty()) continue;

                String segLc = seg.toLowerCase(Locale.ROOT);

                // 2.1 Changement de statut ("marque X comme/en …" ou "mets X en …")
                if (segLc.contains("marque") || (segLc.startsWith("mets ") && containsStatusToken(segLc))) {
                    String status = detectStatus(segLc);
                    if (status != null) {
                        for (String t : extractTitlesForStatus(seg, segLc)) {
                            if (!t.isBlank()) plan.add(new SetStatus(t, status));
                        }
                        continue;
                    }
                }

                // 2.2 Ajout à la wishlist (ne PAS exiger "liste/wishlist" : "ajoute Alien, Heat" doit marcher)
                if (containsAny(segLc, ADD_VERBS)) {
                    List<String> titles = extractTitlesAroundVerb(seg, segLc, ADD_VERBS,
                            Set.of(" à ", " a ", " dans ", " sur "));
                    if (titles.isEmpty()) {
                        // fallback si pas trouvé après le verbe : on tente un split direct du segment
                        titles = splitAndSanitize(QUOTES.matcher(seg).replaceAll(""));
                    }
                    for (String t : titles) {
                        if (!t.isBlank()) plan.add(new Add(t));
                    }
                    continue;
                }


                // 2.3 Suppression / retrait
                if (containsAny(segLc, REMOVE_VERBS)) {
                    for (String t : extractTitlesAroundVerb(seg, segLc, REMOVE_VERBS,
                            Set.of(" de ", " du ", " de la ", " de ma ", " de mon "))) {
                        if (!t.isBlank()) plan.add(new Remove(t));
                    }
                    continue;
                }

                // 2.4 Fallback : guillemets -> ajout
                List<String> quoted = extractQuotedTitles(seg);
                if (!quoted.isEmpty()) {
                    for (String t : quoted) plan.add(new Add(t));
                }
            }

            return plan;
        }

        private static boolean containsAny(String s, Set<String> needles) {
            for (String n : needles) if (s.contains(n)) return true;
            return false;
        }

        private static boolean containsStatusToken(String sLc) {
            for (String k : STATUS_ALIASES.keySet()) if (sLc.contains(k)) return true;
            return false;
        }

        private static String detectStatus(String segLc) {
            for (var e : STATUS_ALIASES.entrySet()) if (segLc.contains(e.getKey())) return e.getValue();
            return null;
        }

        private static List<String> extractQuotedTitles(String text) {
            List<String> out = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean in = false;
            for (char c : text.toCharArray()) {
                if (QUOTES.matcher(Character.toString(c)).find()) {
                    in = !in;
                    if (!in) {
                        String t = sanitize(cur.toString());
                        if (!t.isBlank()) out.add(t);
                        cur.setLength(0);
                    }
                } else if (in) {
                    cur.append(c);
                }
            }
            return out;
        }

        /** Extraction après un verbe, jusqu’à une préposition d’arrêt. */
        private static List<String> extractTitlesAroundVerb(String seg, String segLc,
                                                            Set<String> verbs, Set<String> stops) {
            int start = -1;
            for (String v : verbs) {
                int i = segLc.indexOf(v + " ");
                if (i >= 0) { start = i + v.length() + 1; break; }
            }
            if (start < 0) return List.of();

            int end = seg.length();
            for (String stop : stops) {
                int idx = segLc.indexOf(stop);
                if (idx >= 0 && idx > start && idx < end) end = idx;
            }

            String slice = seg.substring(start, end);

            // On prend les titres guillemetés ET les non guillemetés
            // On deduplique en conservant l'ordre d'apparition
            java.util.Set<String> titles = new java.util.LinkedHashSet<>();

            // 1) D’abord les titres entre guillemets (prioritaires)
            titles.addAll(extractQuotedTitles(slice));

            // 2) Puis le reste (CSV / "et"), en supprimant juste les caractères de guillemets
            titles.addAll(splitAndSanitize(QUOTES.matcher(slice).replaceAll("")));

            // Certains parseurs peuvent réintroduire du bruit : on filtre les vides
            titles.removeIf(t -> t == null || t.isBlank());

            return new java.util.ArrayList<>(titles);
        }


        /** Pour "marque X comme/en STATUS" ou "mets X en STATUS". */
        private static List<String> extractTitlesForStatus(String seg, String segLc) {
            int pivot = Math.max(segLc.indexOf(" comme "), segLc.indexOf(" en "));
            String slice = pivot > 0 ? seg.substring(0, pivot) : seg;
            slice = slice.replaceFirst("(?i)^(marque|mets|mettre)\\s+", "");
            List<String> quoted = extractQuotedTitles(slice);
            if (!quoted.isEmpty()) return quoted;
            return splitAndSanitize(slice);
        }

        private static List<String> splitAndSanitize(String text) {
            String norm = text.replace(" et ", ",");
            String[] parts = norm.split(",");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                String t = sanitize(p);
                if (!t.isBlank()) out.add(t);
            }
            return out;
        }

        private static String sanitize(String s) {
            if (s == null) return "";
            return QUOTES.matcher(s).replaceAll("")
                    .replaceAll("\\b(ma|mon|la|le|les|de|du|des|dans|à|a|au|aux|liste|wishlist|d'envie)\\b", "")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }

    // =============== 3) ORCHESTRATEUR : exécute le plan ===============

    static final class Orchestrator {

        static String execute(List<Action> plan) {
            if (plan == null || plan.isEmpty()) return "Aucune action à effectuer.";

            // éviter les doublons (ex: "ajoute A et ajoute A")
            Set<String> added = new LinkedHashSet<>();
            Set<String> removed = new LinkedHashSet<>();
            List<String> statusChanged = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (Action a : plan) {
                try {
                    if (a instanceof Add add) {
                        JsonStorage.addOrUpdate(add.title(), ENVIE);
                        added.add(add.title());
                    } else if (a instanceof Remove rem) {
                        JsonStorage.addOrUpdate(rem.title(), PAS_INTERESSE);
                        removed.add(rem.title());
                    } else if (a instanceof SetStatus ss) {
                        String status = normalizeStatus(ss.status());
                        if (status == null) {
                            errors.add(ss.title() + " → statut invalide");
                        } else {
                            JsonStorage.addOrUpdate(ss.title(), status);
                            statusChanged.add(ss.title() + " → " + status);
                        }
                    }
                } catch (Exception e) {
                    errors.add(a + " : " + e.getMessage());
                }
            }

            // Résumé clair
            StringBuilder sb = new StringBuilder();
            if (!added.isEmpty())   sb.append("Ajoutés: ").append(String.join(", ", added)).append(". ");
            if (!removed.isEmpty()) sb.append("Retirés: ").append(String.join(", ", removed)).append(". ");
            if (!statusChanged.isEmpty()) sb.append("Statuts: ").append(String.join(", ", statusChanged)).append(". ");
            if (!errors.isEmpty())  sb.append("Erreurs: ").append(String.join(" | ", errors)).append(". ");
            if (sb.length() == 0)   sb.append("Aucune action reconnue.");
            return sb.toString().trim();
        }

        private static String normalizeStatus(String s) {
            if (s == null) return null;
            String key = s.toLowerCase(Locale.ROOT).trim();
            return STATUS_ALIASES.getOrDefault(key, switch (key) {
                case ENVIE, DEJA_VU, PAS_INTERESSE -> key;
                default -> null;
            });
        }
    }

    // =============== 4) OUTIL LLM : point d’entrée LangChain4j ===============

    @Tool("Exécute plusieurs actions à la fois (ajouts, suppressions, changements de statut). " +
            "Utiliser pour les consignes combinées (mots clés: ajoute/mets/supprime... et/puis/;).")
    public String mixedActions(String instruction) {
        List<Action> plan = Parser.parse(instruction);
        return Orchestrator.execute(plan);
    }

    public static boolean shouldForceMulti(String s) {
        if (s == null) return false;
        String lc = s.toLowerCase(Locale.ROOT);
        boolean hasJoin = lc.contains(" et ") || lc.contains(" puis ") || lc.indexOf(';') >= 0 || lc.indexOf('.') >= 0;
        boolean hasVerb = lc.contains("ajout") || lc.contains("mets ") || lc.contains("met ") ||
                lc.contains("marque") || lc.contains("supprim") || lc.contains("retir") || lc.contains("enlèv") || lc.contains("enlev");
        return hasJoin && hasVerb;
    }
}

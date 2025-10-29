package app.cinematch.agent;

import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Représente la mémoire utilisateur persistante pour les recommandations de films.
 *
 * <p>Cette classe enregistre et charge les préférences de l’utilisateur via {@link JsonStorage}.
 * Les films sont classés selon trois statuts :
 * <ul>
 *     <li><b>déjà vu</b> — films que l’utilisateur a regardés</li>
 *     <li><b>envie</b> — films que l’utilisateur souhaite voir</li>
 *     <li><b>pas intéressé</b> — films que l’utilisateur ne souhaite pas voir</li>
 * </ul>
 *
 * <p>La mémoire est considérée comme <i>stateless</i> : elle ne conserve pas d’état en RAM,
 * mais s’appuie sur une couche de stockage persistante (JSON local).
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * Memory memory = new Memory();
 * memory.addSeen("Inception");
 * memory.addToWatch("Interstellar");
 * List<String> envies = memory.toWatch();
 * }</pre>
 *
 * @see app.cinematch.util.JsonStorage
 * @see app.cinematch.model.HistoryEntry
 */
public class Memory {

    /**
     * Ajoute ou met à jour un film avec le statut {@code "deja_vu"} (déjà vu).
     *
     * @param title le titre du film à marquer comme vu
     */
    public void addSeen(final String title) {
        JsonStorage.addOrUpdate(title, "deja_vu");
    }

    /**
     * Ajoute ou met à jour un film avec le statut {@code "envie"} (à voir).
     *
     * @param title le titre du film que l’utilisateur souhaite voir
     */
    public void addToWatch(final String title) {
        JsonStorage.addOrUpdate(title, "envie");
    }

    /**
     * Ajoute ou met à jour un film avec le statut {@code "pas_interesse"} (non apprécié).
     *
     * @param title le titre du film que l’utilisateur ne souhaite pas voir
     */
    public void addNotInterested(final String title) {
        JsonStorage.addOrUpdate(title, "pas_interesse");
    }

    /**
     * Retourne la liste des titres marqués comme {@code "déjà vus"}.
     *
     * <p>Les doublons sont supprimés pour garantir une liste unique.</p>
     *
     * @return la liste des films déjà vus
     */
    public List<String> seen() {
        return JsonStorage.loadAll().stream()
                .filter(e -> "deja_vu".equalsIgnoreCase(e.status()))
                .map(HistoryEntry::title)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des titres marqués comme {@code "envie"} (à voir).
     *
     * @return la liste des films à voir
     */
    public List<String> toWatch() {
        return JsonStorage.loadAll().stream()
                .filter(e -> "envie".equalsIgnoreCase(e.status()))
                .map(HistoryEntry::title)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des titres marqués comme {@code "pas_interesse"} (non appréciés).
     *
     * @return la liste des films que l’utilisateur ne souhaite pas voir
     */
    public List<String> notInterested() {
        return JsonStorage.loadAll().stream()
                .filter(e -> "pas_interesse".equalsIgnoreCase(e.status()))
                .map(HistoryEntry::title)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste complète des entrées mémorisées, quel que soit leur statut.
     *
     * @return l’ensemble de l’historique utilisateur
     */
    public List<HistoryEntry> history() {
        return JsonStorage.loadAll();
    }

    /**
     * Retourne une représentation textuelle de la mémoire utilisateur.
     *
     * @return un résumé indiquant le nombre de films vus, désirés et non appréciés
     */
    @Override
    public String toString() {
        return "Mémoire : " + seen().size() + " vus, "
                + toWatch().size() + " envies, "
                + notInterested().size() + " pas intéressés.";
    }
}

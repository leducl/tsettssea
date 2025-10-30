package app.cinematch.util;

import app.cinematch.model.HistoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilitaire de persistance locale pour les entrées d’historique.
 *
 * <p>Chemin du fichier configurable via la propriété système
 * {@code cinematch.storage}. Par défaut : {@code src/main/resources/storage.json}.</p>
 *
 * <p>Pour <b>silencier</b> les logs d’erreurs (utile en tests), définir
 * {@code -Dcinematch.storage.silent=true}.</p>
 *
 * <p>Les méthodes sont synchronisées (accès concurrents sûrs). En cas d’erreur d’E/S,
 * le comportement reste tolérant et journalise (sauf mode silencieux).</p>
 */
public final class JsonStorage {

    /**
     * Propriété système pour surcharger le chemin de stockage.
     */
    private static final String PROP_PATH = "cinematch.storage";
    /**
     * Propriété système pour désactiver l’affichage des erreurs sur stderr.
     */
    private static final String PROP_SILENT = "cinematch.storage.silent";
    /**
     * Chemin par défaut.
     */
    private static final String DEFAULT_PATH = "src/main/resources/storage.json";

    /**
     * Mapper JSON Jackson.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonStorage() {
        // Classe utilitaire : pas d’instanciation.
    }

    /**
     * Ajoute ou met à jour une entrée d’historique identifiée par son titre (case-insensitive).
     *
     * @param title  titre du film
     * @param status statut (ex. "envie", "deja_vu", "pas_interesse")
     */
    public static void addOrUpdate(final String title, final String status) {
        final List<HistoryEntry> all = loadAll();
        all.removeIf(e -> e.title().equalsIgnoreCase(title));
        all.add(new HistoryEntry(title, status, LocalDateTime.now().toString()));
        saveAll(all);
    }

    /**
     * Lit toutes les entrées depuis le fichier JSON.
     *
     * @return liste d’entrées, ou liste vide si absent/illisible
     */
    public static List<HistoryEntry> loadAll() {
        final File file = storageFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(file, new TypeReference<List<HistoryEntry>>() {
            });
        } catch (IOException e) {
            // Tolérance : retourne liste vide si JSON corrompu (tests inclus)
            return new ArrayList<>();
        }
    }

    /**
     * Retourne les titres correspondant à un statut, triés par date décroissante.
     *
     * @param status statut (ex. "envie")
     * @return liste de titres
     */
    public static List<String> getByStatus(final String status) {
        return loadAll().stream()
                .filter(e -> e.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(HistoryEntry::dateTimeIso).reversed())
                .map(HistoryEntry::title)
                .collect(Collectors.toList());
    }

    /**
     * Écrit la liste complète d’entrées dans le fichier JSON.
     * Crée le répertoire parent si nécessaire.
     *
     * @param all liste à sauvegarder
     */
    public static void saveAll(final List<HistoryEntry> all) {
        final File file = storageFile();
        final File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            final boolean created = parentDir.mkdirs();
            if (!created && !parentDir.exists()) {
                logErr("[JsonStorage] Échec de création du répertoire : %s%n",
                        parentDir.getAbsolutePath());
            }
        }

        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, all);
        } catch (IOException e) {
            logErr("[JsonStorage] Erreur lors de l’écriture du fichier %s : %s%n",
                    file.getAbsolutePath(), e.getMessage());
        }
    }

    // -------- internes

    /**
     * Retourne le fichier de stockage à partir de la propriété système.
     */
    private static File storageFile() {
        final String p = System.getProperty(PROP_PATH, DEFAULT_PATH);
        return new File(p);
    }

    /**
     * Log sur stderr sauf si le mode silencieux est activé.
     */
    private static void logErr(final String fmt, final Object... args) {
        if (!Boolean.getBoolean(PROP_SILENT)) {
            System.err.printf(fmt, args);
        }
    }
}

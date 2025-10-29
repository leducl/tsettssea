package app.cinematch.model;

/**
 * Représente une entrée d’historique pour un film dans la mémoire de l’utilisateur.
 *
 * <p>Chaque instance contient trois informations principales :
 * <ul>
 *     <li><b>title</b> — le titre du film concerné</li>
 *     <li><b>status</b> — l’état associé à ce film (ex. :
 *         {@code "deja_vu"}, {@code "envie"}, {@code "pas_interesse"})</li>
 *     <li><b>dateTimeIso</b> — la date et l’heure de l’enregistrement,
 *         au format ISO 8601 (ex. : {@code "2025-10-24T09:30:00"})</li>
 * </ul>
 *
 * <p>Cette classe est un {@link java.lang.Record}, ce qui la rend immuable et adaptée
 * à la sérialisation JSON ou au stockage persistant via {@code JsonStorage}.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * HistoryEntry entry = new HistoryEntry("Inception", "deja_vu", "2025-10-24T09:30:00");
 * System.out.println(entry.title()); // Affiche "Inception"
 * }</pre>
 *
 * @param title        le titre du film
 * @param status       le statut du film (déjà vu, envie, pas intéressé, etc.)
 * @param dateTimeIso  la date et l’heure au format ISO 8601
 */
public record HistoryEntry(String title, String status, String dateTimeIso) { }

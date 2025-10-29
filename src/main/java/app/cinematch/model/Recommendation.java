package app.cinematch.model;

/**
 * Représente une recommandation de film générée par l’agent conversationnel.
 *
 * <p>Chaque recommandation comprend :
 * <ul>
 *   <li><b>title</b> — le titre du film recommandé</li>
 *   <li><b>reason</b> — la raison ou justification de la recommandation (ex. : "même réalisateur que...")</li>
 *   <li><b>platform</b> — la plateforme où le film peut être visionné (ex. : Netflix, Prime Video, etc.)</li>
 *   <li><b>posterUrl</b> — l’URL de l’affiche du film (pour affichage graphique)</li>
 * </ul>
 *
 * <p>Ce record est immuable et facilement sérialisable,
 * ce qui le rend adapté à la communication entre le moteur de recommandation
 * et les composants d’interface utilisateur.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * Recommendation rec = new Recommendation(
 *     "Inception",
 *     "Un thriller psychologique captivant du même réalisateur que Interstellar.",
 *     "Netflix",
 *     "https://image.tmdb.org/t/p/inception.jpg"
 * );
 * System.out.println(rec.title()); // Affiche "Inception"
 * }</pre>
 *
 * @param title      le titre du film recommandé
 * @param reason     la raison justifiant la recommandation
 * @param platform   la plateforme de visionnage suggérée (peut être {@code null} ou vide)
 * @param posterUrl  l’URL de l’affiche du film (peut être {@code null} ou vide)
 */
public record Recommendation(String title, String reason, String platform, String posterUrl) { }

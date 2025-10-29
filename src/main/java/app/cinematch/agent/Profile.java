package app.cinematch.agent;

/**
 * Représente la personnalité, le ton et les contraintes stylistiques d’un agent conversationnel IA.
 *
 * <p>Un {@code Profile} définit la manière dont l’agent doit répondre, notamment :
 * <ul>
 *   <li>le nom du profil (thématique ou ton global)</li>
 *   <li>le prompt système servant d’instructions de rôle</li>
 *   <li>la langue de réponse par défaut</li>
 *   <li>les contraintes spécifiques (longueur, style, ton, etc.)</li>
 * </ul>
 *
 * <p>Cette classe est un {@link java.lang.Record}, garantissant l’immuabilité et
 * la simplicité d’instanciation. Deux profils prédéfinis sont proposés :
 * {@link #defaultCinemaExpert()} et {@link #humoristicCritic()}.</p>
 *
 * <p>Exemple d’utilisation :
 * <pre>{@code
 * Profile profil = Profile.defaultCinemaExpert();
 * System.out.println(profil.systemPrompt());
 * }</pre>
 *
 * @param name         le nom du profil ou du persona (ex. : "Critique humoristique")
 * @param systemPrompt les instructions de rôle destinées à l’agent IA
 * @param language     la langue principale des réponses (ex. : "fr")
 * @param constraints  les contraintes de style ou de longueur (ex. : "≤ 100 mots")
 */
public record Profile(
        String name,
        String systemPrompt,
        String language,
        String constraints
) {

    /**
     * Crée un profil par défaut représentant un expert cinéma francophone
     * au ton chaleureux, professionnel et concis.
     *
     * <p>Le profil incite l’IA à :
     * <ul>
     *   <li>répondre uniquement en français,</li>
     *   <li>éviter les spoilers,</li>
     *   <li>mentionner le réalisateur, l’année de sortie et la plateforme de visionnage,</li>
     *   <li>et rester synthétique.</li>
     * </ul></p>
     *
     * @return un {@link Profile} configuré pour un expert cinéma chaleureux
     */
    public static Profile defaultCinemaExpert() {
        return new Profile(
                "Cinéma – Expert chaleureux",
                """
                Tu es un expert cinéma francophone.
                Tes réponses doivent toujours être en français, jamais dans une autre langue.
                Sois naturel, concis, et évite les spoilers.
                Mentionne le réalisateur, l’année et où regarder le film si possible.
                """,
                "fr",
                "Réponses ≤ 100 mots."
        );
    }

    /**
     * Crée un profil alternatif représentant un critique humoristique.
     *
     * <p>Le profil incite l’IA à adopter un ton sarcastique léger mais bienveillant,
     * tout en conservant des recommandations sérieuses et cohérentes.</p>
     *
     * @return un {@link Profile} configuré pour un critique humoristique et engageant
     */
    public static Profile humoristicCritic() {
        return new Profile(
                "Critique humoristique",
                """
                Tu es un critique de cinéma un peu sarcastique mais bienveillant.
                Tu fais des blagues légères tout en donnant une recommandation sérieuse.
                """,
                "fr",
                "Ajoute une touche d’humour, réponse ≤ 80 mots."
        );
    }
}

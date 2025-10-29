package app.cinematch.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires de Profile (record).
 * 100 % de couverture JaCoCo, sans avertissements Checkstyle/SpotBugs.
 * Scénarios commentés en Given / When / Then.
 */
final class ProfileTest {

    @Test
    @DisplayName("defaultCinemaExpert() – retourne le profil expert cinéma francophone attendu")
    void defaultCinemaExpert_returnsExpectedProfile() {
        // Given aucun prérequis

        // When on crée le profil par défaut
        final Profile p = Profile.defaultCinemaExpert();

        // Then tous les champs correspondent aux attentes
        assertEquals("Cinéma – Expert chaleureux", p.name());
        assertEquals("fr", p.language());
        assertEquals("Réponses ≤ 100 mots.", p.constraints());

        final String prompt = p.systemPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("Tu es un expert cinéma francophone."));
        assertTrue(prompt.contains("Tes réponses doivent toujours être en français"));
        assertTrue(prompt.contains("Sois naturel, concis, et évite les spoilers."));
        assertTrue(prompt.contains("Mentionne le réalisateur, l’année et où regarder le film si possible."));
    }

    @Test
    @DisplayName("humoristicCritic() – retourne le profil critique humoristique attendu")
    void humoristicCritic_returnsExpectedProfile() {
        // Given aucun prérequis

        // When on crée le profil humoristique
        final Profile p = Profile.humoristicCritic();

        // Then tous les champs correspondent aux attentes
        assertEquals("Critique humoristique", p.name());
        assertEquals("fr", p.language());
        assertEquals("Ajoute une touche d’humour, réponse ≤ 80 mots.", p.constraints());

        final String prompt = p.systemPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("Tu es un critique de cinéma un peu sarcastique mais bienveillant."));
        assertTrue(prompt.contains("Tu fais des blagues légères tout en donnant une recommandation sérieuse."));
    }

    @Test
    @DisplayName("Record – égalité structurelle, composants accessibles, toString non nul")
    void record_semantics_equality_components_toString() {
        // Given deux profils identiques créés depuis la factory
        final Profile p1 = Profile.defaultCinemaExpert();
        final Profile p2 = Profile.defaultCinemaExpert();

        // When on crée un profil différent en ne changeant qu'une contrainte
        final Profile p3 = new Profile(
                p1.name(),
                p1.systemPrompt(),
                p1.language(),
                "Autre contrainte"
        );

        // Then égalité structurelle entre p1 et p2; p3 différent
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);

        // THEN composants accessibles et cohérents
        assertNotNull(p1.name());
        assertNotNull(p1.systemPrompt());
        assertEquals("fr", p1.language());

        // THEN toString() non nul et informatif
        assertNotNull(p1.toString());
        assertTrue(p1.toString().contains("Cinéma – Expert chaleureux"));
    }
}

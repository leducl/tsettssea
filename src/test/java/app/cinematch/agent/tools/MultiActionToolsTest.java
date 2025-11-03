package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class MultiActionToolsTest {

    @Test
    @DisplayName("Add + Remove dans une même consigne")
    void givenAddAndRemove_whenMixed_thenBothApplied() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — On prépare l'orchestrateur et la phrase combinant deux actions.
            MultiActionTools tools = new MultiActionTools();
            String instruction = "ajoute Drive à ma liste et supprime Dune de ma liste";

            // WHEN — On exécute la fonctionnalité testée.
            String out = tools.mixedActions(instruction);

            // THEN — On vérifie que les deux opérations ont bien été déclenchées, dans le bon statut.
            js.verify(() -> JsonStorage.addOrUpdate("Drive", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Dune", "pas_interesse"));
            assertTrue(out.contains("Ajoutés"));
            assertTrue(out.contains("Retirés"));
        }
    }

    @Test
    @DisplayName("CSV + 'et' + guillemets → tout doit être ajouté, puis retrait")
    void givenCsvAndQuotes_whenMixed_thenAllAddedAndRemoved() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — On construit une consigne avec liste CSV, ‘et’, guillemets + un retrait.
            MultiActionTools tools = new MultiActionTools();
            String instruction = "ajoute Alien, Heat et \"Blade Runner 2049\" à ma wishlist; puis supprime Parasite.";

            // WHEN — Exécution.
            String out = tools.mixedActions(instruction);

            // THEN — Vérification des appels et d'un résumé lisible.
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"), times(1));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "envie"), times(1));
            js.verify(() -> JsonStorage.addOrUpdate("Blade Runner 2049", "envie"), times(1));
            js.verify(() -> JsonStorage.addOrUpdate("Parasite", "pas_interesse"), times(1));
            assertTrue(out.contains("Ajoutés"));
            assertTrue(out.contains("Retirés"));
        }
    }

    @Test
    @DisplayName("Changement de statuts (déjà vu / pas intéressé)")
    void givenStatusChanges_whenMixed_thenStatusesUpdated() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — Une consigne qui change deux statuts différents.
            MultiActionTools tools = new MultiActionTools();
            String instruction = "marque \"Jojo Rabbit\" comme déjà vu et mets Matrix en pas_interesse";

            // WHEN — Exécution.
            String out = tools.mixedActions(instruction);

            // THEN — Vérifie les statuts normalisés.
            js.verify(() -> JsonStorage.addOrUpdate("Jojo Rabbit", "deja_vu"));
            js.verify(() -> JsonStorage.addOrUpdate("Matrix", "pas_interesse"));
            assertTrue(out.contains("Statuts"));
        }
    }

    @Test
    @DisplayName("Fallback : un titre uniquement entre guillemets → ajout en 'envie'")
    void givenOnlyQuoted_whenMixed_thenFallbackAdd() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — Pas de verbe explicite, juste un titre entre guillemets.
            MultiActionTools tools = new MultiActionTools();
            String instruction = "\"Dune\"";

            // WHEN — Exécution.
            String out = tools.mixedActions(instruction);

            // THEN — Ajout implicite à la wishlist.
            js.verify(() -> JsonStorage.addOrUpdate("Dune", "envie"));
            assertTrue(out.contains("Ajoutés"));
        }
    }

    @Test
    @DisplayName("Guillemets typographiques + stopwords FR → titres bien nettoyés")
    void givenSmartQuotes_whenMixed_thenSanitized() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — Guillemets « » / “ ” + petits mots à ignorer.
            MultiActionTools tools = new MultiActionTools();
            String instruction = "ajoute “Le Grand Badaud” à ma liste et supprime « Pain and Chocolate » de ma liste";

            // WHEN — Exécution.
            tools.mixedActions(instruction);

            // THEN — Titres nettoyés et statuts corrects.
            js.verify(() -> JsonStorage.addOrUpdate("Le Grand Badaud", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Pain and Chocolate", "pas_interesse"));
        }
    }

    @Test
    @DisplayName("Alias de statuts 'déjà vu' / 'deja vu' reconnus")
    void givenStatusAliases_whenMixed_thenDetected() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — Variantes orthographiques du même statut.
            MultiActionTools tools = new MultiActionTools();

            // WHEN — Deux exécutions avec variantes.
            tools.mixedActions("mets Alien en déjà-vu");
            tools.mixedActions("mets Heat en deja vu");

            // THEN — Les deux doivent être normalisés en 'deja_vu'.
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "deja_vu"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "deja_vu"));
        }
    }

    @Test
    @DisplayName("Null / blanc → pas d'action")
    void givenNullOrBlank_whenMixed_thenNoop() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — Entrées nulles ou vides.
            MultiActionTools tools = new MultiActionTools();

            // WHEN — Exécution avec null et blanc.
            String out1 = tools.mixedActions(null);
            String out2 = tools.mixedActions("   ");

            // THEN — Aucune interaction et message d'absence d'action.
            js.verifyNoInteractions();
            assertTrue(out1.toLowerCase().contains("aucune"));
            assertTrue(out2.toLowerCase().contains("aucune"));
        }
    }

    @Test
    @DisplayName("Phrase sans action → 'Aucune action reconnue.'")
    void givenNoAction_whenMixed_thenGraceful() {
        try (MockedStatic<JsonStorage> js = mockStatic(JsonStorage.class)) {

            // GIVEN — Texte conversationnel sans verbe d'action.
            MultiActionTools tools = new MultiActionTools();
            String instruction = "bonjour, ça va ?";

            // WHEN — Exécution.
            String out = tools.mixedActions(instruction);

            // THEN — Pas d’appels et message par défaut.
            js.verifyNoInteractions();
            assertEquals("Aucune action à effectuer.", out);
        }
    }
}

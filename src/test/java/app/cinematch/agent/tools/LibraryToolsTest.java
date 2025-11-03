package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de LibraryTools en style GIVEN / WHEN / THEN,
 */
class LibraryToolsTest {

    // --------- markAsSeen ---------

    @Test
    @DisplayName("markAsSeen - simple -> 'deja_vu' + SEEN:<title>")
    void givenTitle_whenMarkAsSeen_thenStoredAsSeen() {
        // GIVEN: JsonStorage mocké & un titre
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN: marquer 'Dune' comme déjà vu
            String res = tools.markAsSeen(" Dune ");

            // THEN: stockage 'deja_vu' et message SEEN:Dune
            js.verify(() -> JsonStorage.addOrUpdate("Dune", "deja_vu"));
            js.verifyNoMoreInteractions();
            assertEquals("SEEN:Dune", res);
        }
    }

    @Test
    @DisplayName("markAsSeen - guillemets/espaces -> normalisé")
    void givenWeirdQuotes_whenMarkAsSeen_thenNormalized() {
        // GIVEN: titre bruité
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN: appel avec « Interstellar »
            String res = tools.markAsSeen("  «Interstellar»  ");

            // THEN: normalisation + statut 'deja_vu'
            js.verify(() -> JsonStorage.addOrUpdate("Interstellar", "deja_vu"));
            js.verifyNoMoreInteractions();
            assertEquals("SEEN:Interstellar", res);
        }
    }

    @Test
    @DisplayName("markAsSeen - blanc/null -> ERROR:EMPTY_TITLE")
    void givenBlankOrNull_whenMarkAsSeen_thenError() {
        // GIVEN: entrées vides
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN: chaîne blanche
            String res1 = tools.markAsSeen("   ");
            // THEN: erreur et aucune écriture
            assertEquals("ERROR:EMPTY_TITLE", res1);
            js.verifyNoInteractions();
        }

        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN: null
            String res2 = tools.markAsSeen(null);

            // THEN: erreur et aucune écriture
            assertEquals("ERROR:EMPTY_TITLE", res2);
            js.verifyNoInteractions();
        }
    }

    // --------- markAsDisliked ---------

    @Test
    @DisplayName("markAsDisliked - simple -> 'pas_interesse' + DISLIKED:<title>")
    void givenTitle_whenMarkAsDisliked_thenStoredAsNope() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.markAsDisliked("\"Matrix\"");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Matrix", "pas_interesse"));
            js.verifyNoMoreInteractions();
            assertEquals("DISLIKED:Matrix", res);
        }
    }

    @Test
    @DisplayName("markAsDisliked - espaces multiples -> normalisé")
    void givenMultiSpaces_whenMarkAsDisliked_thenNormalized() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.markAsDisliked(" The   Host ");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("The Host", "pas_interesse"));
            js.verifyNoMoreInteractions();
            assertEquals("DISLIKED:The Host", res);
        }
    }

    @Test
    @DisplayName("markAsDisliked - blanc/null -> ERROR:EMPTY_TITLE")
    void givenBlankOrNull_whenMarkAsDisliked_thenError() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.markAsDisliked("   ");

            // THEN
            assertEquals("ERROR:EMPTY_TITLE", res);
            js.verifyNoInteractions();
        }
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.markAsDisliked(null);

            // THEN
            assertEquals("ERROR:EMPTY_TITLE", res);
            js.verifyNoInteractions();
        }
    }

    // --------- setStatus ---------

    @Test
    @DisplayName("setStatus - statut supporté -> applique exactement le statut")
    void givenTitleAndValidStatus_whenSetStatus_thenUpdated() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus(" Jojo Rabbit ", "pas_interesse");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Jojo Rabbit", "pas_interesse"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_CHANGED:Jojo Rabbit->pas_interesse", res);
        }
    }

    @Test
    @DisplayName("setStatus - casse/espaces -> normalise 'DEJA_VU' en 'deja_vu'")
    void givenUppercaseStatus_whenSetStatus_thenNormalized() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus(" Heat ", "  DEJA_VU  ");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "deja_vu"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_CHANGED:Heat->deja_vu", res);
        }
    }

    @Test
    @DisplayName("setStatus - inconnu -> fallback 'envie'")
    void givenUnknownStatus_whenSetStatus_thenFallbackToEnvie() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus(" Alien ", "???");

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_CHANGED:Alien->envie", res);
        }
    }

    @Test
    @DisplayName("setStatus - statut null -> par défaut 'envie'")
    void givenNullStatus_whenSetStatus_thenDefaultEnvie() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus(" Drive ", null);

            // THEN
            js.verify(() -> JsonStorage.addOrUpdate("Drive", "envie"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_CHANGED:Drive->envie", res);
        }
    }

    @Test
    @DisplayName("setStatus - titre blanc/null -> ERROR:EMPTY_TITLE")
    void givenBlankOrNullTitle_whenSetStatus_thenError() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus("   ", "envie");

            // THEN
            assertEquals("ERROR:EMPTY_TITLE", res);
            js.verifyNoInteractions();
        }
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            LibraryTools tools = new LibraryTools(mock(MovieRecommenderService.class));

            // WHEN
            String res = tools.setStatus(null, "envie");

            // THEN
            assertEquals("ERROR:EMPTY_TITLE", res);
            js.verifyNoInteractions();
        }
    }

    // --------- generateDescription ---------

    @Test
    @DisplayName("generateDescription - délègue au MovieRecommenderService")
    void givenTitle_whenGenerateDescription_thenDelegatesToService() {
        // GIVEN: service mocké et stub de réponse
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        when(svc.generateDescription("Alien")).thenReturn("Desc courte Alien");
        LibraryTools tools = new LibraryTools(svc);

        // WHEN: demande de description
        String res = tools.generateDescription(" Alien ");

        // THEN: la méthode du service est appelée et sa réponse renvoyée
        assertEquals("Desc courte Alien", res);
        verify(svc).generateDescription("Alien");
        verifyNoMoreInteractions(svc);
    }

    @Test
    @DisplayName("generateDescription - blanc/null -> ERROR:EMPTY_TITLE et service non appelé")
    void givenBlankOrNull_whenGenerateDescription_thenErrorAndNoCall() {
        // GIVEN: service mocké
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LibraryTools tools = new LibraryTools(svc);

        // WHEN: titre blanc
        String r1 = tools.generateDescription("   ");
        // THEN: erreur et aucune délégation
        assertEquals("ERROR:EMPTY_TITLE", r1);
        verify(svc, never()).generateDescription(anyString());

        // WHEN: titre null
        String r2 = tools.generateDescription(null);
        // THEN: erreur et toujours aucun appel
        assertEquals("ERROR:EMPTY_TITLE", r2);
        verify(svc, never()).generateDescription(anyString());
        verifyNoMoreInteractions(svc);
    }

    // --------- Couverture helpers privés (norm / normStatus) via réflexion ---------

    @Test
    @DisplayName("norm - null -> \"\" ; guillemets & doubles espaces -> nettoyé")
    void givenVariousInputs_whenNorm_thenCleans() throws Exception {
        // GIVEN: accès à la méthode privée statique
        Method norm = LibraryTools.class.getDeclaredMethod("norm", String.class);
        norm.setAccessible(true);

        // WHEN / THEN                                                               // WHEN / THEN
        assertEquals("",        norm.invoke(null, (Object) null));                   // THEN: null -> ""
        assertEquals("Matrix",  norm.invoke(null, "\"Matrix\""));                    // THEN: guillemets retirés
        assertEquals("The Host",norm.invoke(null, " The   Host "));                  // THEN: espaces multiples réduits
        assertEquals("Dune",    norm.invoke(null, "« Dune »"));                      // THEN: guillemets typographiques
    }

    @Test
    @DisplayName("normStatus - valide/inconnu/null -> branches couvertes")
    void givenStatuses_whenNormStatus_thenAllBranches() throws Exception {
        // GIVEN: accès à la méthode privée statique              // GIVEN
        Method ns = LibraryTools.class.getDeclaredMethod("normStatus", String.class);
        ns.setAccessible(true);

        // WHEN / THEN: valeurs valides
        assertEquals("envie",         ns.invoke(null, "envie"));
        assertEquals("pas_interesse", ns.invoke(null, "pas_interesse"));
        assertEquals("deja_vu",       ns.invoke(null, "deja_vu"));

        // WHEN / THEN: casse + espaces
        assertEquals("pas_interesse", ns.invoke(null, "  PAS_INTERESSE  "));

        // WHEN / THEN: inconnu -> fallback
        assertEquals("envie",         ns.invoke(null, "???"));

        // WHEN / THEN: null -> défaut
        assertEquals("envie",         ns.invoke(null, (Object) null));
    }
}

package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de WishlistTools suivant le pattern GIVEN / WHEN / THEN,
 * avec commentaires "GIVEN / WHEN / THEN" à côté des étapes.
 */
class WishlistToolsTest {

    private WishlistTools tools;

    @BeforeEach
    void setUp() {
        tools = new WishlistTools();
    }

    // --------------------------- addToWishlist ---------------------------

    @Test
    @DisplayName("addToWishlist - titre simple -> ADDED:<title>")
    void givenValidTitle_whenAddToWishlist_thenReturnsAddedMessageAndCallsJsonStorage() {
        // GIVEN: un titre valide                                                // GIVEN
        String title = " Inception ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: on ajoute à la wishlist                                      // WHEN
            String result = tools.addToWishlist(title);

            // THEN: stockage appelé & message ADDED                              // THEN
            mocked.verify(() -> JsonStorage.addOrUpdate("Inception", "envie"));
            mocked.verifyNoMoreInteractions();
            assertEquals("ADDED:Inception", result);
        }
    }

    @Test
    @DisplayName("addToWishlist - CSV + \\n -> ADDED_MANY:n (filtre les vides)")
    void givenCsvAndNewline_whenAddToWishlist_thenAddedMany() {
        // GIVEN: plusieurs titres séparés par virgule/retour ligne              // GIVEN
        String multi = "Alien,  ,Heat\nDrive";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: ajout multiple (branche 'contains(,\\n)')                    // WHEN
            String result = tools.addToWishlist(multi);

            // THEN: 3 écritures non vides, résumé ADDED_MANY:3                   // THEN
            mocked.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            mocked.verify(() -> JsonStorage.addOrUpdate("Heat", "envie"));
            mocked.verify(() -> JsonStorage.addOrUpdate("Drive", "envie"));
            mocked.verifyNoMoreInteractions();
            assertEquals("ADDED_MANY:3", result);
        }
    }

    @Test
    @DisplayName("addToWishlist - tokens tous vides -> ADDED_MANY:0")
    void givenOnlyEmptyTokens_whenAddToWishlist_thenZero() {
        // GIVEN: entrée composée uniquement d'éléments vides                     // GIVEN
        String multi = " ,  ,  \n  ,  ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: ajout multiple                                                // WHEN
            String result = tools.addToWishlist(multi);

            // THEN: aucune interaction & compteur 0                              // THEN
            mocked.verifyNoInteractions();
            assertEquals("ADDED_MANY:0", result);
        }
    }

    @Test
    @DisplayName("addToWishlist - titre vide -> ERROR:EMPTY_TITLE")
    void givenBlankTitle_whenAddToWishlist_thenReturnsErrorAndDoesNotCallStorage() {
        // GIVEN: chaîne blanche                                                  // GIVEN
        String title = "   ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: tentative d'ajout                                            // WHEN
            String result = tools.addToWishlist(title);

            // THEN: pas d'écriture & erreur                                      // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    @Test
    @DisplayName("addToWishlist - titre null -> ERROR:EMPTY_TITLE")
    void givenNullTitle_whenAddToWishlist_thenReturnsError() {
        // GIVEN: null                                                            // GIVEN
        String title = null;

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: tentative d'ajout                                            // WHEN
            String result = tools.addToWishlist(title);

            // THEN: pas d'écriture & erreur                                      // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    // --------------------------- removeFromWishlist ---------------------------

    @Test
    @DisplayName("removeFromWishlist - titre simple -> REMOVED:<title> (pas_interesse)")
    void givenValidTitle_whenRemoveFromWishlist_thenReturnsRemovedMessage() {
        // GIVEN: un titre valide                                                 // GIVEN
        String title = " Titanic ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: on retire de la wishlist                                     // WHEN
            String result = tools.removeFromWishlist(title);

            // THEN: statut 'pas_interesse' appliqué & message REMOVED            // THEN
            mocked.verify(() -> JsonStorage.addOrUpdate("Titanic", "pas_interesse"));
            mocked.verifyNoMoreInteractions();
            assertEquals("REMOVED:Titanic", result);
        }
    }

    @Test
    @DisplayName("removeFromWishlist - blanc -> ERROR:EMPTY_TITLE")
    void givenBlankTitle_whenRemoveFromWishlist_thenReturnsError() {
        // GIVEN: chaîne blanche                                                  // GIVEN
        String title = "   ";

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: tentative de retrait                                         // WHEN
            String result = tools.removeFromWishlist(title);

            // THEN: pas d'écriture & erreur                                      // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    @Test
    @DisplayName("removeFromWishlist - null -> ERROR:EMPTY_TITLE")
    void givenNullTitle_whenRemoveFromWishlist_thenReturnsError() {
        // GIVEN: null                                                            // GIVEN
        String title = null;

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            // WHEN: tentative de retrait                                         // WHEN
            String result = tools.removeFromWishlist(title);

            // THEN: pas d'écriture & erreur                                      // THEN
            mocked.verifyNoInteractions();
            assertEquals("ERROR:EMPTY_TITLE", result);
        }
    }

    // --------------------------- getListByStatus ---------------------------

    @Test
    @DisplayName("getListByStatus - 'envie' -> normalise, filtre vides, distinct")
    void givenStatus_whenGetListByStatus_thenReturnsNormalizedFilteredList() {
        // GIVEN: données bruyantes renvoyées par le stockage                     // GIVEN
        List<String> mockData = Arrays.asList(" Inception ", "\"Titanic\"", "  ");
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie")).thenReturn(mockData);

            // WHEN: récupération de la liste pour 'envie'                        // WHEN
            List<String> result = tools.getListByStatus("envie");

            // THEN: titres normalisés, vides exclus, doublons retirés            // THEN
            assertEquals(List.of("Inception", "Titanic"), result);
        }
    }

    @Test
    @DisplayName("getListByStatus - statut en MAJ + espaces -> toLowerCase + trim")
    void givenUppercaseStatus_whenGetListByStatus_thenLowercasedIsUsed() {
        // GIVEN: stub sur 'pas_interesse'                                        // GIVEN
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("pas_interesse"))
                    .thenReturn(Collections.singletonList("  « Dune »  "));

            // WHEN: appel avec '  PAS_INTERESSE  '                               // WHEN
            List<String> result = tools.getListByStatus("  PAS_INTERESSE  ");

            // THEN: getByStatus('pas_interesse') appelé et valeur normalisée     // THEN
            mocked.verify(() -> JsonStorage.getByStatus("pas_interesse"));
            assertEquals(List.of("Dune"), result);
        }
    }

    @Test
    @DisplayName("getListByStatus - statut blanc -> défaut 'envie'")
    void givenBlankStatus_whenGetListByStatus_thenDefaultsToEnvie() {
        // GIVEN: aucune intention explicite                                      // GIVEN
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(Collections.singletonList("Interstellar"));

            // WHEN: statut blanc                                                  // WHEN
            List<String> result = tools.getListByStatus("  ");

            // THEN: fallback sur 'envie'                                          // THEN
            mocked.verify(() -> JsonStorage.getByStatus("envie"));
            assertEquals(List.of("Interstellar"), result);
        }
    }

    @Test
    @DisplayName("getListByStatus - statut null -> défaut 'envie'")
    void givenNullStatus_whenGetListByStatus_thenDefaultsToEnvie() {
        // GIVEN: null                                                            // GIVEN
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(Collections.singletonList("Dune"));

            // WHEN: statut null                                                   // WHEN
            List<String> result = tools.getListByStatus(null);

            // THEN: fallback sur 'envie'                                          // THEN
            mocked.verify(() -> JsonStorage.getByStatus("envie"));
            assertEquals(List.of("Dune"), result);
        }
    }

    @Test
    @DisplayName("getListByStatus - doublons -> distinct")
    void givenDuplicates_whenGetListByStatus_thenRemovesDuplicates() {
        // GIVEN: plusieurs variantes du même titre                               // GIVEN
        List<String> mockData = Arrays.asList(" Inception ", "Inception", "Inception  ");
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie")).thenReturn(mockData);

            // WHEN: on récupère 'envie'                                           // WHEN
            List<String> result = tools.getListByStatus("envie");

            // THEN: un seul 'Inception'                                           // THEN
            assertEquals(List.of("Inception"), result);
        }
    }

    // --------------------------- normalize (privée) ---------------------------

    @Test
    @DisplayName("normalize - nettoie guillemets, trim, espaces multiples")
    void givenVariousStrings_whenNormalize_thenCleansCorrectly() throws Exception {
        // GIVEN: accès à la méthode privée via réflexion                         // GIVEN
        var normalize = WishlistTools.class.getDeclaredMethod("normalize", String.class);
        normalize.setAccessible(true);

        // WHEN/THEN: différents cas                                               // WHEN / THEN
        assertEquals("Inception", normalize.invoke(tools, "  Inception  "));        // THEN
        assertEquals("Titanic",   normalize.invoke(tools, "\"Titanic\""));          // THEN
        assertEquals("Dune",      normalize.invoke(tools, "« Dune »"));             // THEN
        assertEquals("",          normalize.invoke(tools, new Object[]{null}));     // THEN
        assertEquals("A B",       normalize.invoke(tools, "A    B"));               // THEN
    }
}

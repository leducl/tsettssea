package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests BDD style (GIVEN / WHEN / THEN) pour 100% de couverture JaCoCo de BulkTools.
 */
class BulkToolsTest {

    // --- Utilitaire pour invoquer les méthodes privées (couverture totale) ---
    private static Object invokePrivate(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = BulkTools.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    // --------------------------- addManyToWishlist ---------------------------

    @Test
    @DisplayName("addManyToWishlist - CSV & \\n -> ajoute tous les titres en 'envie'")
    void givenCsv_whenAddMany_thenAllEnvie() {
        // GIVEN: JsonStorage mocké & une liste de titres
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: ajout multiple
            String res = tools.addManyToWishlist("Alien, Heat\nDrive");

            // THEN: chaque titre est marqué 'envie' et le résumé indique 3 ajouts
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Drive", "envie"));
            js.verifyNoMoreInteractions();
            assertEquals("ADDED_MANY:3", res);
        }
    }

    @Test
    @DisplayName("addManyToWishlist - guillemets & espaces multiples -> normalisation")
    void givenQuotedAndSpaced_whenAddMany_thenNormalizedAdded() {
        // GIVEN: titres à normaliser (guillemets typographiques, espaces multiples)
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();
            String titles = " \"Alien\" , «Heat» , “Drive” , The   Driver ";

            // WHEN: ajout après normalisation
            String res = tools.addManyToWishlist(titles);

            // THEN: guillemets retirés, espaces réduits, 4 ajouts
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Drive", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("The Driver", "envie"));
            js.verifyNoMoreInteractions();
            assertEquals("ADDED_MANY:4", res);
        }
    }

    @Test
    @DisplayName("addManyToWishlist - éléments vides -> aucun ajout")
    void givenOnlyBlanks_whenAddMany_thenIgnored() {
        // GIVEN: liste où tout devient vide après normalisation
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: tentative d'ajout
            String res = tools.addManyToWishlist("  ,  , \"   \" , \n  ");

            // THEN: aucune interaction et compteur à 0
            js.verifyNoInteractions();
            assertEquals("ADDED_MANY:0", res);
        }
    }

    @Test
    @DisplayName("addManyToWishlist - null -> 0 ajout")
    void givenNullTitles_whenAddMany_thenZero() {
        // GIVEN: entrée null
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: appel
            String res = tools.addManyToWishlist(null);

            // THEN: aucune écriture et 0
            js.verifyNoInteractions();
            assertEquals("ADDED_MANY:0", res);
        }
    }

    // --------------------------- removeManyFromWishlist ---------------------------

    @Test
    @DisplayName("removeManyFromWishlist - mix CSV/\\n/vides -> ne garde que valides")
    void givenMessyList_whenRemoveMany_thenOnlyValidPasInteresse() {
        // GIVEN: liste bruitée
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();
            String titles = "Alien\n\n , Heat,   , \n";

            // WHEN: suppression en lot
            String res = tools.removeManyFromWishlist(titles);

            // THEN: seuls titres non vides marqués 'pas_interesse'
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "pas_interesse"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "pas_interesse"));
            js.verifyNoMoreInteractions();
            assertEquals("REMOVED_MANY:2", res);
        }
    }

    @Test
    @DisplayName("removeManyFromWishlist - null -> 0")
    void givenNullTitles_whenRemoveMany_thenZero() {
        // GIVEN: entrée null
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: appel
            String res = tools.removeManyFromWishlist(null);

            // THEN: aucune écriture et 0
            js.verifyNoInteractions();
            assertEquals("REMOVED_MANY:0", res);
        }
    }

    // --------------------------- setManyStatus ---------------------------

    @Test
    @DisplayName("setManyStatus - CSV -> applique 'deja_vu' à chaque titre")
    void givenCsv_whenSetManyStatus_thenAllUpdated() {
        // GIVEN: JsonStorage mocké & des titres
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: on applique le statut 'deja_vu' en lot
            String res = tools.setManyStatus("Alien,Heat", "deja_vu");

            // THEN: chaque titre mis à jour & résumé correct
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "deja_vu"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "deja_vu"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_MANY:2->deja_vu", res);
        }
    }

    @Test
    @DisplayName("setManyStatus - casse/espaces -> normalise 'pas_interesse'")
    void givenWeirdCasing_whenSetManyStatus_thenNormalizedPasInteresse() {
        // GIVEN: statut en majuscules + espaces
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: application du statut
            String res = tools.setManyStatus("Alien", "  PAS_INTERESSE ");

            // THEN: normalisé et résumé
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "pas_interesse"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_MANY:1->pas_interesse", res);
        }
    }

    @Test
    @DisplayName("setManyStatus - statut inconnu -> fallback 'envie'")
    void givenUnknownStatus_whenSetManyStatus_thenFallbackEnvie() {
        // GIVEN: statut non supporté
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: application du statut
            String res = tools.setManyStatus("Alien,Heat", "???");

            // THEN: repli sur 'envie' et résumé
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "envie"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_MANY:2->envie", res);
        }
    }

    @Test
    @DisplayName("setManyStatus - statut null -> 'envie'")
    void givenNullStatus_whenSetManyStatus_thenDefaultEnvie() {
        // GIVEN: statut null
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: application
            String res = tools.setManyStatus("Alien", null);

            // THEN: défaut 'envie'
            js.verify(() -> JsonStorage.addOrUpdate("Alien", "envie"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_MANY:1->envie", res);
        }
    }

    @Test
    @DisplayName("setManyStatus - titres null -> 0, statut conservé dans le résumé")
    void givenNullTitles_whenSetManyStatus_thenZeroAndKeepsStatusInSummary() {
        // GIVEN: titres null
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: application
            String res = tools.setManyStatus(null, "deja_vu");

            // THEN: aucune écriture et 0
            js.verifyNoInteractions();
            assertEquals("STATUS_MANY:0->deja_vu", res);
        }
    }

    @Test
    @DisplayName("setManyStatus - 'DEJA_VU' -> normalisé en 'deja_vu'")
    void givenUppercaseDejaVu_whenSetManyStatus_thenNormalized() {
        // GIVEN: casse différente
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            BulkTools tools = new BulkTools();

            // WHEN: application
            String res = tools.setManyStatus("Heat", "DEJA_VU");

            // THEN: normalisé en 'deja_vu'
            js.verify(() -> JsonStorage.addOrUpdate("Heat", "deja_vu"));
            js.verifyNoMoreInteractions();
            assertEquals("STATUS_MANY:1->deja_vu", res);
        }
    }

    // --------------------------- Couverture helpers privés via réflexion ---------------------------

    @Test
    @DisplayName("split - null -> tableau vide")
    void givenNull_whenSplit_thenEmptyArray() throws Exception {
        // GIVEN: appel direct via réflexion
        // WHEN: split(null)
        String[] out = (String[]) invokePrivate("split", new Class[]{String.class}, (Object) null);

        // THEN: non null et longueur 0
        assertNotNull(out);
        assertEquals(0, out.length);
    }

    @Test
    @DisplayName("norm - null -> chaîne vide")
    void givenNull_whenNorm_thenEmptyString() throws Exception {
        // GIVEN: appel via réflexion
        // WHEN: norm(null)
        String out = (String) invokePrivate("norm", new Class[]{String.class}, (Object) null);

        // THEN: vide
        assertEquals("", out);
    }

    @Test
    @DisplayName("norm - guillemets & doubles espaces -> nettoyé")
    void givenQuotedAndDoubleSpaces_whenNorm_thenCleaned() throws Exception {
        // GIVEN: texte avec guillemets typographiques et espaces multiples
        // WHEN: norm(texte)
        String out = (String) invokePrivate("norm", new Class[]{String.class}, "  «The   Driver»  ");

        // THEN: guillemets retirés & espaces réduits
        assertEquals("The Driver", out);
    }

    @Test
    @DisplayName("normStatus - inconnu -> 'envie'")
    void givenUnknown_whenNormStatus_thenEnvie() throws Exception {
        // GIVEN: statut inconnu
        // WHEN: normStatus(unknown)
        String out = (String) invokePrivate("normStatus", new Class[]{String.class}, "unknown");

        // THEN: fallback 'envie'
        assertEquals("envie", out);
    }

    @Test
    @DisplayName("normStatus - null -> 'envie'")
    void givenNull_whenNormStatus_thenEnvie() throws Exception {
        // GIVEN: null
        // WHEN: normStatus(null)
        String out = (String) invokePrivate("normStatus", new Class[]{String.class}, (Object) null);

        // THEN: défaut 'envie'
        assertEquals("envie", out);
    }

    @Test
    @DisplayName("normStatus - valeurs valides inchangées")
    void givenValidStatuses_whenNormStatus_thenKept() throws Exception {
        // GIVEN: valeurs valides
        // WHEN/THEN: restent identiques
        assertEquals("envie", (String) invokePrivate("normStatus", new Class[]{String.class}, "envie"));
        assertEquals("pas_interesse", (String) invokePrivate("normStatus", new Class[]{String.class}, "pas_interesse"));
        assertEquals("deja_vu", (String) invokePrivate("normStatus", new Class[]{String.class}, "deja_vu"));
    }
}

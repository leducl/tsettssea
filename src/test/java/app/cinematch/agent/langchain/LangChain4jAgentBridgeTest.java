package app.cinematch.agent.langchain;

import app.cinematch.MovieRecommenderService;
import app.cinematch.agent.Profile;
import app.cinematch.agent.tools.BulkTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests LangChain4jAgentBridge en style GIVEN / WHEN / THEN
 * (commentaires "GIVEN / WHEN / THEN" au plus près des étapes).
 *
 * On vérifie le pré-parseur client (tryClientSideBulkAdd) et l’extraction des titres FR/EN.
 * Aucun appel LLM réel n’est effectué.
 */
class LangChain4jAgentBridgeTest {

    /** Utilitaire pour setter un champ privé (ici, bulkTools). */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ---------------------------------------------------------------------
    // ask(...) — déclenchement du pré-parseur (cas FR avec virgules)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("ask - pré-parseur: 'Ajoute A, B, C à ma wishlist' -> addManyToWishlist + message '(3).'")
    void givenBulkAddSentence_whenAsk_thenClientSideBulkIsTriggered() throws Exception {
        // GIVEN: bridge réel, BulkTools espionné pour vérifier l'appel               // GIVEN
        String url = "http://localhost:11434";
        String model = "llama3.1:8b-instruct";
        MovieRecommenderService svc = Mockito.mock(MovieRecommenderService.class);

        LangChain4jAgentBridge bridge =
                new LangChain4jAgentBridge(url, model, Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = Mockito.spy(new BulkTools());
        setPrivateField(bridge, "bulkTools", spyBulk);

        // WHEN: message utilisateur avec plusieurs titres                            // WHEN
        String out = bridge.ask("Ajoute Alien, Heat, Drive à ma wishlist");

        // THEN: le pré-parseur court-circuite l'appel LLM                            // THEN
        verify(spyBulk, times(1)).addManyToWishlist("Alien, Heat, Drive");
        assertTrue(out.contains("(3)."), "Le message doit indiquer (3).");
        assertTrue(out.toLowerCase().contains("ajout"), "Doit contenir 'Ajouté(s)'.");
    }

    // ---------------------------------------------------------------------
    // ask(...) — pré-parseur avec guillemets/retours ligne (FR)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("ask - pré-parseur: titres sur plusieurs lignes + guillemets -> '(3).'")
    void givenBulkAddWithQuotesAndNewlines_whenAsk_thenClientSideBulkWorks() throws Exception {
        // GIVEN                                                                     // GIVEN
        String url = "http://localhost:11434";
        String model = "llama3.1:8b-instruct";
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge =
                new LangChain4jAgentBridge(url, model, Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = spy(new BulkTools());
        setPrivateField(bridge, "bulkTools", spyBulk);

        String prompt = "Mets \"Alien\",\n«Heat», Drive dans la liste d'envie";

        // WHEN                                                                       // WHEN
        String out = bridge.ask(prompt);

        // THEN: l’appel groupé a lieu et le message récap indique 3 ajouts          // THEN
        verify(spyBulk, times(1)).addManyToWishlist(anyString());
        assertTrue(out.contains("(3)."), "Le message doit indiquer (3).");
    }

    // ---------------------------------------------------------------------
    // tryClientSideBulkAdd(...) — pas de séparateurs -> null
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("tryClientSideBulkAdd - pas de virgule/\\n -> null (pas de lot)")
    void givenNoCommaNorNewline_whenTryClientSideBulkAdd_thenNull() throws Exception {
        // GIVEN: bridge et accès à la méthode privée                                // GIVEN
        String url = "http://localhost:11434";
        String model = "llama3.1:8b-instruct";
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge =
                new LangChain4jAgentBridge(url, model, Profile.defaultCinemaExpert(), svc);

        var m = LangChain4jAgentBridge.class.getDeclaredMethod("tryClientSideBulkAdd", String.class);
        m.setAccessible(true);

        // WHEN: phrase qui ressemble à un ajout mais sans séparateurs                // WHEN
        Object res = m.invoke(bridge, "Ajoute Alien à ma wishlist");

        // THEN: pas de traitement client -> null                                     // THEN
        assertNull(res);
    }

    // ---------------------------------------------------------------------
    // tryClientSideBulkAdd(...) — titres vides -> null
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("tryClientSideBulkAdd - 'Ajoute à ma wishlist' (titres vides) -> null")
    void givenNoTitles_whenTryClientSideBulkAdd_thenNull() throws Exception {
        // GIVEN                                                                      // GIVEN
        String url = "http://localhost:11434";
        String model = "llama3.1:8b-instruct";
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge =
                new LangChain4jAgentBridge(url, model, Profile.defaultCinemaExpert(), svc);

        var m = LangChain4jAgentBridge.class.getDeclaredMethod("tryClientSideBulkAdd", String.class);
        m.setAccessible(true);

        // WHEN: rien entre 'ajoute' et 'à ma wishlist'                                // WHEN
        Object res = m.invoke(bridge, "Ajoute   à ma wishlist");

        // THEN: extraction vide -> null                                              // THEN
        assertNull(res);
    }

    // ---------------------------------------------------------------------
    // tryClientSideBulkAdd(...) — anglais "Add A, B to wishlist"
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("tryClientSideBulkAdd - anglais 'Add A, B to wishlist' -> addManyToWishlist('A, B') & '(2).'")
    void givenEnglishAdd_whenTryClientSideBulkAdd_thenOk() throws Exception {
        // GIVEN                                                                      // GIVEN
        String url = "http://localhost:11434";
        String model = "llama3.1:8b-instruct";
        MovieRecommenderService svc = mock(MovieRecommenderService.class);
        LangChain4jAgentBridge bridge =
                new LangChain4jAgentBridge(url, model, Profile.defaultCinemaExpert(), svc);

        BulkTools spyBulk = spy(new BulkTools());
        setPrivateField(bridge, "bulkTools", spyBulk);

        var m = LangChain4jAgentBridge.class.getDeclaredMethod("tryClientSideBulkAdd", String.class);
        m.setAccessible(true);

        // WHEN: message en anglais                                                   // WHEN
        String out = (String) m.invoke(bridge, "Add Alien, Heat to wishlist");

        // THEN: addManyToWishlist appelé avec la bonne chaîne & message '(2).'       // THEN
        verify(spyBulk, times(1)).addManyToWishlist("Alien, Heat");
        assertTrue(out.endsWith("(2)."), "La réponse doit indiquer (2).");
    }
}

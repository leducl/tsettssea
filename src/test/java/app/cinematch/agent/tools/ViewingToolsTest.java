package app.cinematch.agent.tools;

import app.cinematch.MovieRecommenderService;
import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Arrays;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests ViewingTools en style GIVEN / WHEN / THEN
 * (commentaires "GIVEN / WHEN / THEN" placés à côté des étapes).
 */
class ViewingToolsTest {

    // --------------------- RANDOM (+desc) ---------------------

    @Test
    @DisplayName("pickNextToWatch - random + description -> 'NEXT:<title> | <desc>'")
    void givenWishlist_whenPickRandomWithDescription_thenReturnsTitleAndDesc() {
        // GIVEN: wishlist non vide & service qui sait décrire
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("Alien"));
            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            when(svc.generateDescription("Alien")).thenReturn("Desc Alien");
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: on demande un choix random avec description
            String res = tools.pickNextToWatch("random", "true");

            // THEN: la réponse contient le titre normalisé et la description
            assertTrue(res.startsWith("NEXT:Alien"));
            assertTrue(res.contains("Desc Alien"));
            verify(svc).generateDescription("Alien");
            verifyNoMoreInteractions(svc);
        }
    }

    // --------------------- RANDOM (sans desc) ---------------------

    @Test
    @DisplayName("pickNextToWatch - random sans description -> 'NEXT:<title>' et service non appelé")
    void givenWishlist_whenPickRandomNoDescription_thenNoServiceCall() {
        // GIVEN: 1 seul élément pour rendre le random déterministe
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("Alien"));
            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: withDescription = false
            String res = tools.pickNextToWatch("random", "false");

            // THEN: pas d'appel au service, juste le titre
            assertEquals("NEXT:Alien", res);
            verify(svc, never()).generateDescription(anyString());
            verifyNoMoreInteractions(svc);
        }
    }

    @Test
    @DisplayName("pickNextToWatch - stratégie inconnue -> fallback random")
    void givenUnknownStrategy_whenPick_thenFallsBackToRandom() {
        // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("Inception"));
            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: stratégie non reconnue
            String res = tools.pickNextToWatch("???", "false");

            // THEN: retourne quand même un 'NEXT:<title>'
            assertEquals("NEXT:Inception", res);
            verify(svc, never()).generateDescription(anyString());
        }
    }

    // --------------------- FIRST ---------------------

    @Test
    @DisplayName("pickNextToWatch - first sans description -> renvoie le premier non vide")
    void givenWishlist_whenPickFirst_thenReturnsFirst() {
        // GIVEN: premier = Heat
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("Heat", "Alien", "Drive"));
            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: stratégie 'first'
            String res = tools.pickNextToWatch("first", "false");

            // THEN: renvoie 'NEXT:Heat' et aucun appel au service
            assertEquals("NEXT:Heat", res);
            verify(svc, never()).generateDescription(anyString());
        }
    }

    @Test
    @DisplayName("pickNextToWatch - 'FiRsT' + 'TrUe' (casse insensible) -> normalisé + desc")
    void givenCaseInsensitivity_whenPickFirstWithDescription_thenWorks() {
        // GIVEN: valeur unique avec guillemets typographiques
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of(" «Drive» "));
            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            when(svc.generateDescription("Drive")).thenReturn("Short desc Drive");
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: stratégie et flag en casse mixte
            String res = tools.pickNextToWatch("FiRsT", "TrUe");

            // THEN: titre nettoyé + description
            assertEquals("NEXT:Drive | Short desc Drive", res);
            verify(svc).generateDescription("Drive");
        }
    }

    @Test
    @DisplayName("pickNextToWatch - first avec liste bruitée -> ignore null/blancs, nettoie guillemets")
    void givenMessyList_whenPickFirst_thenCleansAndSkipsBlanks() {
        // GIVEN: la liste contient null/vides + un premier élément valide encadré de guillemets  // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(Arrays.asList(null, "  ", "  \"Heat\"  ", "\"Drive\"")); // <-- idem

            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: stratégie 'first' (prend le premier non vide, puis normalise)               // WHEN
            String res = tools.pickNextToWatch("first", "false");

            // THEN: titre nettoyé sans guillemets -> 'Heat', service non appelé                  // THEN
            assertEquals("NEXT:Heat", res);
            verify(svc, never()).generateDescription(anyString());
        }
    }


    // --------------------- LISTES VIDES ---------------------

    @Test
    @DisplayName("pickNextToWatch - wishlist vide -> NEXT:EMPTY")
    void givenEmptyWishlist_whenPick_thenEmpty() {
        // GIVEN: aucune envie
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of());
            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            ViewingTools tools = new ViewingTools(svc);

            // WHEN
            String res = tools.pickNextToWatch("first", "true");

            // THEN: aucun choix possible
            assertEquals("NEXT:EMPTY", res);
            verify(svc, never()).generateDescription(anyString());
        }
    }

    @Test
    @DisplayName("pickNextToWatch - tous les éléments sont blancs/null -> NEXT:EMPTY")
    void givenAllBlankOrNull_whenPick_thenEmpty() {
        // GIVEN: uniquement des entrées vides/null dans la wishlist                         // GIVEN
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(Arrays.asList(null, "", "   ")); // <-- List.of remplacé par Arrays.asList

            MovieRecommenderService svc = mock(MovieRecommenderService.class);
            ViewingTools tools = new ViewingTools(svc);

            // WHEN: on tente de choisir un film, peu importe la stratégie/desc               // WHEN
            String res = tools.pickNextToWatch("random", "true");

            // THEN: rien à proposer -> NEXT:EMPTY, et aucun appel au service                 // THEN
            assertEquals("NEXT:EMPTY", res);
            verify(svc, never()).generateDescription(anyString());
        }
    }

}

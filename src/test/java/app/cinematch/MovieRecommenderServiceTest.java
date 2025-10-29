package app.cinematch;

import app.cinematch.api.OllamaClient;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorageMock;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de MovieRecommenderService.
 * Style BDD (Given / When / Then) avec commentaires explicatifs.
 */
public class MovieRecommenderServiceTest {

    /**
     * Faux client Ollama pour les tests.
     * - N'appelle jamais le réseau
     * - Défile les réponses simulées dans RESPONSES
     */
    private static final class FakeOllamaClient extends OllamaClient {
        static final Deque<String> RESPONSES = new ArrayDeque<>();

        FakeOllamaClient() {
            super("http://fake", "test-model");
        }

        @Override
        public String chat(final String system, final String user) {
            return RESPONSES.isEmpty() ? "" : RESPONSES.removeFirst();
        }

        static void reset() {
            RESPONSES.clear();
        }
    }

    // Méthode utilitaire pour instancier le service avec injection du faux client
    private MovieRecommenderService newService() {
        final OllamaClient fake = new FakeOllamaClient();
        final BiConsumer<String, String> sink = JsonStorageMock::addOrUpdate;
        return new MovieRecommenderService(fake, sink);
    }

    // Nettoyage entre les tests
    @AfterEach
    void tearDown() {
        FakeOllamaClient.reset();
        JsonStorageMock.reset();
    }

    // -------------------------------------------------------------------------
    // GIVEN / WHEN / THEN tests avec explications détaillées
    // -------------------------------------------------------------------------

    @Test
    void givenFullJson_whenRecommendFromLike_thenBuildsRecommendationWithYearAndSuffix() {
        // GIVEN : l’IA renvoie un JSON complet avec tous les champs.
        FakeOllamaClient.RESPONSES.add("""
        {
          "title":"Inception",
          "pitch":"Un casse onirique audacieux",
          "year":"2010",
          "platform":"Netflix"
        }
        """);
        final MovieRecommenderService service = newService();

        // WHEN : on demande une recommandation à partir du film "Interstellar".
        final Recommendation result = service.recommendFromLike("Interstellar");

        // THEN : le service construit une recommandation complète :
        // titre, pitch, année, suffixe ajouté, plateforme.
        assertEquals("Inception", result.title());
        assertTrue(result.reason().contains("Un casse onirique audacieux"));
        assertTrue(result.reason().contains("année suggérée : 2010"));
        assertTrue(result.reason().contains("Inspiré de Interstellar"));
        assertEquals("Netflix", result.platform());
        assertNull(result.posterUrl());
    }

    @Test
    void givenPitchAlreadyMentionsLikedTitle_whenRecommendFromLike_thenNoSuffixIsAdded() {
        // GIVEN : le pitch contient déjà le titre du film aimé.
        FakeOllamaClient.RESPONSES.add("""
        {
          "title":"Blade Runner",
          "pitch":"Hommage appuyé à Interstellar dans ses thèmes.",
          "year":"1982",
          "platform":"Club Cinéphile"
        }
        """);
        final MovieRecommenderService service = newService();

        // WHEN : on appelle recommendFromLike("Interstellar").
        final Recommendation result = service.recommendFromLike("Interstellar");

        // THEN : le pitch d’origine est conservé, sans suffixe ajouté.
        assertTrue(result.reason().startsWith("Hommage appuyé à Interstellar"));
        assertTrue(result.reason().contains("année suggérée : 1982"));
        assertEquals("Blade Runner", result.title());
        assertEquals("Club Cinéphile", result.platform());
    }

    @Test
    void givenNoJson_whenRecommendRandom_thenUsesFallbacks() {
        // GIVEN : la réponse n’est pas un JSON, juste une ligne de texte.
        FakeOllamaClient.RESPONSES.add("  - 🎥 The Matrix\nDu texte en plus\n");
        final MovieRecommenderService service = newService();

        // WHEN : on appelle recommendRandom().
        final Recommendation result = service.recommendRandom();

        // THEN : le titre est extrait de la première ligne lisible
        // et le reste utilise les valeurs par défaut.
        assertEquals("🎥 The Matrix", result.title());
        assertEquals("Suggestion IA", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenPartialJson_whenRecommendRandom_thenUsesFallbackTitleAndPlatform() {
        // GIVEN : JSON partiel avec champs vides.
        FakeOllamaClient.RESPONSES.add("""
        Intro de l'IA
        {"title":"   ","pitch":"Pitch présent","year":"  ","platform":"   "}
        Ligne finale
        """);
        final MovieRecommenderService service = newService();

        // WHEN : on appelle recommendRandom().
        final Recommendation result = service.recommendRandom();

        // THEN : le titre provient de la première ligne non vide,
        // le pitch est pris du JSON, et la plateforme est choisie aléatoirement.
        assertEquals("Intro de l'IA", result.title());
        assertEquals("Pitch présent", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenInvalidJsonStructure_whenRecommendRandom_thenCatchesParseError() {
        // GIVEN : JSON malformé (branche catch du parse()).
        FakeOllamaClient.RESPONSES.add("intro { \"title\": OOPS, \"pitch\": , } fin");
        final MovieRecommenderService service = newService();

        // WHEN : on appelle recommendRandom().
        final Recommendation result = service.recommendRandom();

        // THEN : le service gère l’erreur et utilise les valeurs fallback.
        assertEquals("intro { \"title\": OOPS, \"pitch\": , } fin", result.title());
        assertEquals("Suggestion IA", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenEmptyResponse_whenRecommendRandom_thenUsesDoubleFallback() {
        // GIVEN : la réponse de l’IA est vide.
        FakeOllamaClient.RESPONSES.add("");
        final MovieRecommenderService service = newService();

        // WHEN : recommendRandom() est appelé.
        final Recommendation result = service.recommendRandom();

        // THEN : le titre devient "Suggestion mystère" et le pitch "Suggestion IA".
        assertEquals("Suggestion mystère", result.title());
        assertEquals("Suggestion IA", result.reason());
        assertTrue(Set.of("Cinéma du Coin+", "StreamFiction", "Club Cinéphile", "Festival Replay")
                .contains(result.platform()));
    }

    @Test
    void givenMovieTitle_whenGenerateDescription_thenReturnsOllamaResponse() {
        // GIVEN : l’IA renvoie une courte description.
        FakeOllamaClient.RESPONSES.add("Une description immersive.");
        final MovieRecommenderService service = newService();

        // WHEN : on appelle generateDescription().
        final String description = service.generateDescription("Parasite");

        // THEN : la méthode retourne exactement le texte renvoyé par l’IA.
        assertEquals("Une description immersive.", description);
    }

    @Test
    void givenTitleAndStatus_whenMark_thenDelegatesToJsonStorage() {
        // GIVEN : service avec un sink mock (JsonStorageMock).
        final MovieRecommenderService service = new MovieRecommenderService(
                new FakeOllamaClient(),
                app.cinematch.util.JsonStorageMock::addOrUpdate
        );

        // WHEN : on marque un film.
        service.mark("Inception", "liked");

        // THEN : le sink a reçu les valeurs.
        assertEquals("Inception", app.cinematch.util.JsonStorageMock.lastTitle);
        assertEquals("liked", app.cinematch.util.JsonStorageMock.lastStatus);
    }

    @Test
    void givenAllNullOrBlankValues_whenFirstNonBlank_thenReturnsEmptyString() throws Exception {
        // GIVEN : tous les paramètres sont nuls ou vides.
        final MovieRecommenderService service = newService();
        final Method m =
                MovieRecommenderService.class.getDeclaredMethod("firstNonBlank", String[].class);
        m.setAccessible(true);

        // WHEN : on appelle la méthode privée via réflexion.
        final Object result = m.invoke(service, (Object) new String[] {null, "   ", "\t"});

        // THEN : la méthode retourne une chaîne vide "".
        assertEquals("", result);
    }

    @Test
    void givenNullRaw_whenExtractFirstMeaningfulLine_thenReturnsEmptyString() throws Exception {
        // GIVEN : on veut tester la branche où raw == null.
        final MovieRecommenderService service = newService();
        final Method m = MovieRecommenderService.class.getDeclaredMethod(
                "extractFirstMeaningfulLine", String.class);
        m.setAccessible(true);

        // WHEN : on invoque la méthode avec null.
        final Object result = m.invoke(service, (Object) null);

        // THEN : la méthode retourne une chaîne vide (aucune ligne exploitable).
        assertEquals("", result);
    }


    @Test
    void givenBaseUrlAndModel_whenConstruct_thenServiceInstantiated() {
        // GIVEN : un modèle et une URL bidons
        String baseUrl = "http://fake";
        String model = "fake-model";

        // WHEN : on crée le service avec le constructeur standard (prod)
        MovieRecommenderService service = new MovieRecommenderService(baseUrl, model);

        // THEN : l’instance est correctement créée
            assertNotNull(service);
    }

    @Test
    void givenBaseUrlModelAndCustomSink_whenConstruct_thenMarkUsesInjectedSink() {
        // GIVEN : un BiConsumer capturant les valeurs passées à mark()
        final String[] holder = new String[2];
        java.util.function.BiConsumer<String, String> sink = (title, status) -> {
            holder[0] = title;
            holder[1] = status;
        };

        // WHEN : on crée le service avec ce sink personnalisé et on appelle mark()
        MovieRecommenderService service =
                new MovieRecommenderService("http://fake", "fake-model", sink);
        service.mark("Matrix", "seen");

        // THEN : le sink a bien été invoqué avec les bons paramètres
        assertEquals("Matrix", holder[0]);
        assertEquals("seen", holder[1]);
    }

    @Test
    void givenOllamaAndNullSink_whenConstruct_thenDefaultSinkBranchIsCovered() {
        // GIVEN : un faux OllamaClient dont la méthode chat() renvoie toujours une chaîne
        OllamaClient fakeClient = new OllamaClient("http://fake", "fake-model") {
            @Override
            public String chat(String system, String user) {
                return "Une réponse simulée";
            }
        };

        // WHEN : on crée le service avec un sink null (branche du constructeur ternaire)
        MovieRecommenderService service = new MovieRecommenderService(fakeClient, null);

        // THEN : la méthode generateDescription() fonctionne avec le client fake
        String desc = service.generateDescription("Avatar");
        assertEquals("Une réponse simulée", desc);
    }
}

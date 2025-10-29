package app.cinematch.agent;

import app.cinematch.api.OllamaClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de ChatAgent avec commentaires Given/When/Then
 * et couverture complète.
 */
final class ChatAgentTest {

    @Test
    @DisplayName("ask() – listes vides -> texte 'aucun film enregistré'")
    void ask_returnsResponse_withEmptyLists_usesAucunFilm() {
        // Given: OllamaClient mock + Memory construit avec listes vides
        final OllamaClient ollama = mock(OllamaClient.class);
        final Profile initialProfile = mock(Profile.class);
        final Memory ignored = new Memory();

        try (MockedConstruction<Memory> construction = mockConstruction(
                Memory.class,
                (mockMem, ctx) -> {
                    when(mockMem.seen()).thenReturn(Collections.emptyList());
                    when(mockMem.toWatch()).thenReturn(Collections.emptyList());
                    when(mockMem.notInterested()).thenReturn(Collections.emptyList());
                })) {

            final ChatAgent agent = new ChatAgent(ollama, initialProfile, ignored);
            final String userPrompt = "Des idées de films récents ?";

            // When: on appelle ask()
            when(ollama.chat(org.mockito.Mockito.anyString(), org.mockito.Mockito.eq(userPrompt)))
                    .thenReturn("Réponse IA");
            final String result = agent.ask(userPrompt);

            // Then: le message système contient 3× 'aucun film enregistré'
            final ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
            verify(ollama).chat(systemCaptor.capture(), org.mockito.Mockito.eq(userPrompt));
            final String systemMsg = systemCaptor.getValue();

            final String marker = "aucun film enregistré";
            final int count = countOccurrences(systemMsg, marker);
            assertEquals(3, count, "Le message système doit contenir 3 fois 'aucun film enregistré'.");
            assertEquals("Réponse IA", result);
        }
    }

    @Test
    @DisplayName("ask() – listes non vides -> jointure avec ', '")
    void ask_returnsResponse_withNonEmptyLists_joinsProperly() {
        // Given: OllamaClient mock + Memory construit avec données
        final OllamaClient ollama = mock(OllamaClient.class);
        final Profile profile = mock(Profile.class);

        final List<String> seen = Arrays.asList("Le Fabuleux Destin d'Amélie Poulain", "La Haine");
        final List<String> wish = Collections.singletonList("Intouchables");
        final List<String> bad  = Arrays.asList("Sharknado", "Les Tuche");

        try (MockedConstruction<Memory> construction = mockConstruction(
                Memory.class,
                (mockMem, ctx) -> {
                    when(mockMem.seen()).thenReturn(seen);
                    when(mockMem.toWatch()).thenReturn(wish);
                    when(mockMem.notInterested()).thenReturn(bad);
                })) {

            final ChatAgent agent = new ChatAgent(ollama, profile, new Memory());
            final String userPrompt = "Propose-moi 3 films.";

            // When
            when(ollama.chat(org.mockito.Mockito.anyString(), org.mockito.Mockito.eq(userPrompt)))
                    .thenReturn("Réponse OK");
            agent.ask(userPrompt);

            // Then
            final ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
            verify(ollama).chat(systemCaptor.capture(), org.mockito.Mockito.eq(userPrompt));
            final String systemMsg = systemCaptor.getValue();

            assertContains(systemMsg, "Le Fabuleux Destin d'Amélie Poulain, La Haine");
            assertContains(systemMsg, "Intouchables");
            assertContains(systemMsg, "Sharknado, Les Tuche");
        }
    }

    @Test
    @DisplayName("getMemory() – renvoie une nouvelle instance (pas de fuite d'état)")
    void getMemory_returnsNewInstanceEachTime() {
        // Given
        final ChatAgent agent = new ChatAgent(mock(OllamaClient.class), mock(Profile.class), new Memory());

        // When
        final Memory m1 = agent.getMemory();
        final Memory m2 = agent.getMemory();

        // Then
        assertNotSame(m1, m2, "getMemory() doit renvoyer une nouvelle instance à chaque appel.");
    }

    @Test
    @DisplayName("setProfile()/getProfile() – mise à jour du profil")
    void setProfile_updatesAndGetProfile_returnsIt() {
        // Given
        final Profile p1 = mock(Profile.class);
        final ChatAgent agent = new ChatAgent(mock(OllamaClient.class), p1, new Memory());

        // When
        final Profile p2 = mock(Profile.class);
        agent.setProfile(p2);

        // Then
        assertEquals(p2, agent.getProfile(), "setProfile() doit remplacer le profil courant.");
    }

    // ---------- Helpers ----------

    private static int countOccurrences(final String haystack, final String needle) {
        int from = 0;
        int count = 0;
        while (true) {
            final int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + needle.length();
        }
    }

    private static void assertContains(final String text, final String expectedPart) {
        if (!text.contains(expectedPart)) {
            throw new AssertionError("Le texte ne contient pas la sous-chaîne attendue : " + expectedPart);
        }
    }
}

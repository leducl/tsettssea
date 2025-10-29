package app.cinematch.agent;

import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de Memory.
 * 100 % JaCoCo, sans avertissements Checkstyle/SpotBugs.
 * Scénarios commentés en Given / When / Then.
 */
final class MemoryTest {

    // --------- Helpers ---------
    private static HistoryEntry mockEntry(final String title, final String status) {
        final HistoryEntry e = mock(HistoryEntry.class);
        when(e.title()).thenReturn(title);
        when(e.status()).thenReturn(status);
        return e;
    }

    @Test
    @DisplayName("addSeen() appelle JsonStorage.addOrUpdate avec status 'deja_vu'")
    void addSeen_callsJsonStorageWithCorrectStatus() {
        // Given
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            final Memory memory = new Memory();

            // When
            memory.addSeen("Inception");

            // Then (vérification via l'API MockedStatic)
            mocked.verify(() -> JsonStorage.addOrUpdate("Inception", "deja_vu"));
        }
    }

    @Test
    @DisplayName("addToWatch() appelle JsonStorage.addOrUpdate avec status 'envie'")
    void addToWatch_callsJsonStorageWithCorrectStatus() {
        // Given
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            final Memory memory = new Memory();

            // When
            memory.addToWatch("Avatar");

            // Then
            mocked.verify(() -> JsonStorage.addOrUpdate("Avatar", "envie"));
        }
    }

    @Test
    @DisplayName("addNotInterested() appelle JsonStorage.addOrUpdate avec status 'pas_interesse'")
    void addNotInterested_callsJsonStorageWithCorrectStatus() {
        // Given
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            final Memory memory = new Memory();

            // When
            memory.addNotInterested("Titanic");

            // Then
            mocked.verify(() -> JsonStorage.addOrUpdate("Titanic", "pas_interesse"));
        }
    }

    @Test
    @DisplayName("seen() filtre les entrées avec status = 'deja_vu'")
    void seen_returnsOnlyEntriesWithStatusDejaVu() {
        // Given
        final HistoryEntry e1 = mockEntry("Film A", "deja_vu");
        final HistoryEntry e2 = mockEntry("Film B", "envie");
        final List<HistoryEntry> data = List.of(e1, e2);

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(JsonStorage::loadAll).thenReturn(data);
            final Memory memory = new Memory();

            // When
            final List<String> result = memory.seen();

            // Then
            assertEquals(List.of("Film A"), result);
        }
    }

    @Test
    @DisplayName("toWatch() filtre les entrées avec status = 'envie'")
    void toWatch_returnsOnlyEntriesWithStatusEnvie() {
        // Given
        final HistoryEntry e1 = mockEntry("Film X", "envie");
        final HistoryEntry e2 = mockEntry("Film Y", "pas_interesse");
        final List<HistoryEntry> data = List.of(e1, e2);

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(JsonStorage::loadAll).thenReturn(data);
            final Memory memory = new Memory();

            // When
            final List<String> result = memory.toWatch();

            // Then
            assertEquals(List.of("Film X"), result);
        }
    }

    @Test
    @DisplayName("notInterested() filtre les entrées avec status = 'pas_interesse'")
    void notInterested_returnsOnlyEntriesWithStatusPasInteresse() {
        // Given
        final HistoryEntry e1 = mockEntry("Film Z", "pas_interesse");
        final HistoryEntry e2 = mockEntry("Film T", "deja_vu");
        final List<HistoryEntry> data = List.of(e1, e2);

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(JsonStorage::loadAll).thenReturn(data);
            final Memory memory = new Memory();

            // When
            final List<String> result = memory.notInterested();

            // Then
            assertEquals(List.of("Film Z"), result);
        }
    }

    @Test
    @DisplayName("history() renvoie la liste complète renvoyée par JsonStorage.loadAll()")
    void history_returnsAllEntriesFromJsonStorage() {
        // Given
        final List<HistoryEntry> data = List.of(
                mockEntry("Film 1", "deja_vu"),
                mockEntry("Film 2", "envie")
        );

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(JsonStorage::loadAll).thenReturn(data);
            final Memory memory = new Memory();

            // When
            final List<HistoryEntry> result = memory.history();

            // Then
            assertEquals(data, result);
        }
    }

    @Test
    @DisplayName("toString() affiche le résumé du nombre d’entrées par catégorie")
    void toString_returnsSummaryWithCounts() {
        // Given
        final List<HistoryEntry> data = List.of(
                mockEntry("Film A", "deja_vu"),
                mockEntry("Film B", "envie"),
                mockEntry("Film C", "pas_interesse")
        );

        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(JsonStorage::loadAll).thenReturn(data);
            final Memory memory = new Memory();

            // When
            final String result = memory.toString();

            // Then
            assertEquals("Mémoire : 1 vus, 1 envies, 1 pas intéressés.", result);
        }
    }
}

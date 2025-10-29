package app.cinematch.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests BDD pour HistoryEntry. */
public class HistoryEntryTest {

    @Test
    void givenValidValues_whenCreateHistoryEntry_thenFieldsAreAccessible() {
        // Given des valeurs valides
        String title = "Inception";
        String status = "liked";
        String iso = "2025-10-21T18:30:00Z";

        // When on construit le record
        HistoryEntry entry = new HistoryEntry(title, status, iso);

        // Then  les accesseurs renvoient les valeurs
        assertEquals(title, entry.title());
        assertEquals(status, entry.status());
        assertEquals(iso, entry.dateTimeIso());
    }

    @Test
    void givenNulls_whenCreateHistoryEntry_thenRecordAcceptsNull() {
        // Given valeurs nulles
        // When
        HistoryEntry entry = new HistoryEntry(null, null, null);

        // Then
        assertNull(entry.title());
        assertNull(entry.status());
        assertNull(entry.dateTimeIso());
    }
}

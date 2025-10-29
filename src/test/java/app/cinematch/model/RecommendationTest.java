package app.cinematch.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests BDD pour Recommendation. */
public class RecommendationTest {

    @Test
    void givenAllFields_whenCreateRecommendation_thenAccessorsReturnSameValues() {
        // Given
        String title = "Matrix";
        String reason = "Chef-d'œuvre SF";
        String platform = "Netflix";
        String poster = "poster.jpg";

        // When
        Recommendation rec = new Recommendation(title, reason, platform, poster);

        // Then
        assertEquals(title, rec.title());
        assertEquals(reason, rec.reason());
        assertEquals(platform, rec.platform());
        assertEquals(poster, rec.posterUrl());
    }

    @Test
    void givenNulls_whenCreateRecommendation_thenAllowed() {
        // Given / When
        Recommendation rec = new Recommendation(null, null, null, null);

        // Then
        assertNull(rec.title());
        assertNull(rec.reason());
        assertNull(rec.platform());
        assertNull(rec.posterUrl());
    }
}

package app.cinematch.agent.tools;

import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceToolsTest {

    @Test
    @DisplayName("pruneBlanksInStatus - supprime entrées vides/invalides")
    void givenBlanks_whenPruneBlanksInStatus_thenMarkedNope() {
        // GIVEN: liste 'envie' avec éléments vides
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("", "  ", "\"\"", "Alien"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: nettoyage
            String res = tools.pruneBlanksInStatus("envie");

            // THEN: au moins une tentative de marquage 'pas_interesse' sur vides & retour 'PRUNED'
            js.verify(() -> JsonStorage.addOrUpdate("", "pas_interesse"), atLeast(1));
            assertTrue(res.startsWith("PRUNED:"));
        }
    }

    @Test
    @DisplayName("renameTitle - copie le statut et marque l'ancien en 'pas_interesse'")
    void givenOldAndNew_whenRenameTitle_thenCopiesStatusAndMarksOldNope() {
        // GIVEN: "Le Samouraï" est déjà en 'envie'
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("Le Samouraï"));
            js.when(() -> JsonStorage.getByStatus("pas_interesse")).thenReturn(List.of());
            js.when(() -> JsonStorage.getByStatus("deja_vu")).thenReturn(List.of());

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: renommer "Le Samourai" -> "Le Samouraï"
            String res = tools.renameTitle("Le Samourai", "Le Samouraï");

            // THEN: nouveau titre garde 'envie', ancien passe 'pas_interesse'
            js.verify(() -> JsonStorage.addOrUpdate("Le Samouraï", "envie"));
            js.verify(() -> JsonStorage.addOrUpdate("Le Samourai", "pas_interesse"));
            assertTrue(res.contains("RENAMED:"));
        }
    }

    @Test
    @DisplayName("getListByStatusSorted - tri ascendant")
    void givenList_whenGetSortedAsc_thenSorted() {
        // GIVEN: liste non triée
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie"))
                    .thenReturn(List.of("Heat", "Alien", "Drive"));

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN: tri asc
            var res = tools.getListByStatusSorted("envie", "asc");

            // THEN: ordre A->Z
            assertEquals(List.of("Alien", "Drive", "Heat"), res);
        }
    }

    @Test
    @DisplayName("getStats - comptes par statut")
    void whenGetStats_thenCountsReturned() {
        // GIVEN: volumes par statut
        try (MockedStatic<JsonStorage> js = Mockito.mockStatic(JsonStorage.class)) {
            js.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of("A"));
            js.when(() -> JsonStorage.getByStatus("pas_interesse")).thenReturn(List.of("B","C"));
            js.when(() -> JsonStorage.getByStatus("deja_vu")).thenReturn(List.of());

            MaintenanceTools tools = new MaintenanceTools();

            // WHEN
            String res = tools.getStats("all");

            // THEN
            assertEquals("STATS: total=3 | envie=1 | pas_interesse=2 | deja_vu=0", res);
        }
    }
}

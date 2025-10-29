package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.HistoryEntry;
import app.cinematch.util.JsonStorage;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests BDD pour HistoryPanel (UI Swing).
 * - On utilise le VRAI JsonStorage (fichier storage.json), mais on le sauvegarde/restaure par test.
 * - On mocke MovieRecommenderService pour éviter tout accès réseau.
 *
 * GIVEN / WHEN / THEN commentés dans chaque test.
 */
class HistoryPanelTest {

    private static final Path STORAGE_PATH =
            Paths.get("src", "main", "resources", "storage.json");

    private byte[] backupBytes;
    private boolean backupExisted;

    private MainFrame dummyFrame;
    private MovieRecommenderService dummyService;

    @BeforeEach
    void setUp() throws Exception {
        // Sauvegarder le storage.json (s'il existe), puis partir d'un fichier vide
        Files.createDirectories(STORAGE_PATH.getParent());
        backupExisted = Files.exists(STORAGE_PATH);
        backupBytes = backupExisted ? Files.readAllBytes(STORAGE_PATH) : null;
        Files.deleteIfExists(STORAGE_PATH);

        // Frame de test : on veut juste capter showCard("home")
        // Si tu as déjà une petite MainFrame de test avec lastShownCard, garde-la.
        dummyFrame = mock(MainFrame.class);
        // Service mocké : HistoryPanel ne devrait pas l'utiliser, mais on évite toute dépendance externe
        dummyService = mock(MovieRecommenderService.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restaure le fichier d'origine
        Files.deleteIfExists(STORAGE_PATH);
        if (backupExisted && backupBytes != null) {
            Files.write(STORAGE_PATH, backupBytes);
        }
    }

    @Test
    void givenNoHistory_whenCreatePanel_thenTableIsEmpty() throws Exception {
        // GIVEN — JsonStorage vide (fichier absent)
        // WHEN — on instancie le panneau
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);

        // THEN — la table doit exister mais être vide
        JTable table = (JTable) getPrivate(panel, "table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        assertEquals(0, model.getRowCount(), "La table doit être vide sans historique");
    }

    @Test
    void givenHistoryEntries_whenLoadHistory_thenTableIsPopulatedAndSortedDescending() throws Exception {
        // GIVEN — une liste de trois entrées désordonnées
        JsonStorage.saveAll(List.of(
                new HistoryEntry("A", "liked", "2023-10-21T18:00:00Z"),
                new HistoryEntry("B", "disliked", "2025-10-21T18:00:00Z"),
                new HistoryEntry("C", "liked", "2024-10-21T18:00:00Z")
        ));

        // WHEN — création du panel (loadHistory() auto-appelé)
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);

        // THEN — la table doit être triée par date décroissante (B, C, A)
        JTable table = (JTable) getPrivate(panel, "table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        assertEquals(3, model.getRowCount());
        assertEquals("B", model.getValueAt(0, 0));
        assertEquals("C", model.getValueAt(1, 0));
        assertEquals("A", model.getValueAt(2, 0));
    }

    @Test
    void givenUserClicksRefresh_whenLoadHistory_thenTableReloads() throws Exception {
        // GIVEN — historique initial
        JsonStorage.saveAll(List.of(
                new HistoryEntry("Matrix", "liked", "2025-10-21T10:00:00Z")
        ));
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);

        // WHEN — on change le contenu du storage et on simule le clic sur "↻ Rafraîchir"
        JsonStorage.saveAll(List.of(
                new HistoryEntry("Inception", "liked", "2025-10-21T18:00:00Z")
        ));
        JButton refreshBtn = (JButton) getPrivate(panel, "refresh");
        refreshBtn.doClick(); // simule un clic utilisateur

        // THEN — la table est mise à jour (Inception en 1ère ligne)
        JTable table = (JTable) getPrivate(panel, "table");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        assertEquals(1, model.getRowCount());
        assertEquals("Inception", model.getValueAt(0, 0));
    }

    @Test
    void givenUserClicksBackButton_whenActionPerformed_thenCallsShowCardHome() throws Exception {
        // GIVEN — création du panneau
        HistoryPanel panel = new HistoryPanel(dummyService, dummyFrame);
        JButton backBtn = (JButton) getPrivate(panel, "backBtn");

        // WHEN — clic sur le bouton retour
        backBtn.doClick();

        // THEN — la frame doit afficher la carte "home"
        verify(dummyFrame, atLeastOnce()).showCard("home");
    }

    /** Utilitaire : récupère un champ privé pour vérifier son contenu. */
    private static Object getPrivate(Object obj, String name) {
        try {
            Field f = obj.getClass().getDeclaredField(name);
            if (!f.canAccess(obj)) {
                f.setAccessible(true);
            }
            return f.get(obj);
        } catch (Exception e) {
            fail("Erreur d'accès au champ privé " + name + ": " + e.getMessage());
            return null;
        }
    }
}

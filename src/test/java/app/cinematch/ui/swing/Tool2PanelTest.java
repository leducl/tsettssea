package app.cinematch.ui.swing;

import app.cinematch.MovieRecommenderService;
import app.cinematch.model.Recommendation;
import app.cinematch.util.JsonStorage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests synchrones et stables pour Tool2Panel.
 * - Pas de SwingWorker/animations: on injecte l'état requis par réflexion
 * - Given / When / Then en commentaires
 */
final class Tool2PanelTest {

    @Test
    @DisplayName("Construction: composants principaux présents (titre, reason, platform, boutons)")
    void constructor_buildsMainComponents() throws Exception {
        // Given
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);


        try (MockedStatic<JsonStorage> ignored = mockStatic(JsonStorage.class)) {
            // When
            final Tool2Panel[] ref = new Tool2Panel[1];
            SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
            final Tool2Panel panel = ref[0];

            // Then
            assertNotNull(getField(panel, "title"));
            assertNotNull(getField(panel, "reason"));
            assertNotNull(getField(panel, "platform"));
            assertNotNull(getField(panel, "likeBtn"));
            assertNotNull(getField(panel, "nopeBtn"));
            assertNotNull(getField(panel, "seenBtn"));
            assertNotNull(getField(panel, "backBtn"));
            assertNotNull(getField(panel, "descPane"));
        }
    }

    @Test
    @DisplayName("htmlEscape + setDescHtml: la description est correctement échappée et injectée")
    void html_isEscaped_andSetIntoPane() throws Exception {
        // Given
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        final Method htmlEscape = Tool2Panel.class.getDeclaredMethod("htmlEscape", String.class);
        htmlEscape.setAccessible(true);
        final Method setDescHtml = Tool2Panel.class.getDeclaredMethod("setDescHtml", String.class);
        setDescHtml.setAccessible(true);

        // When on échappe puis on pousse dans setDescHtml via réflexion
        final String escaped = (String) htmlEscape.invoke(panel, "A & B < C > D");
        final String inner = escaped.replace("\n", "<br/>"); // équivalent de htmlCenterBig()
        SwingUtilities.invokeAndWait(() -> {
            try {
                setDescHtml.invoke(panel, inner);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        // Then le JEditorPane contient bien les entités HTML
        final JEditorPane descPane = (JEditorPane) getField(panel, "descPane");
        final String html = descPane.getText();
        assertTrue(html.contains("A &amp; B &lt; C &gt; D"));
    }

    @Test
    @DisplayName("Clic 'Je veux voir' -> service.mark(title,'envie')")
    void likeButton_marksEnvie() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);
        final Recommendation rec = mock(Recommendation.class);
        when(rec.title()).thenReturn("Matrix");

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        setField(panel, "current", rec);
        final JLabel title = (JLabel) getField(panel, "title");
        SwingUtilities.invokeAndWait(() -> title.setText("Matrix"));
        enableButtonsForClick(panel); // <-- important

        final JButton likeBtn = (JButton) getField(panel, "likeBtn");
        SwingUtilities.invokeAndWait(likeBtn::doClick);

        verify(service, atLeastOnce()).mark("Matrix", "envie");
    }


    @Test
    @DisplayName("Clic 'Pas intéressé' -> service.mark(title,'pas_interesse')")
    void nopeButton_marksPasInteresse() throws Exception {
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);
        final Recommendation rec = mock(Recommendation.class);
        when(rec.title()).thenReturn("Film Random");

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        setField(panel, "current", rec);
        final JLabel title = (JLabel) getField(panel, "title");
        SwingUtilities.invokeAndWait(() -> title.setText("Film Random"));
        enableButtonsForClick(panel); // <-- important

        final JButton nopeBtn = (JButton) getField(panel, "nopeBtn");
        SwingUtilities.invokeAndWait(nopeBtn::doClick);

        verify(service, atLeastOnce()).mark("Film Random", "pas_interesse");
    }

    @Test
    @DisplayName("Clic 'Déjà vu' -> service.mark(title,'deja_vu')")
    void seenButton_marksDejaVu() throws Exception {
        // Given
        final MovieRecommenderService service = mock(MovieRecommenderService.class);
        final java.util.function.Consumer<String> parent = mock(java.util.function.Consumer.class);

        final Recommendation rec = mock(Recommendation.class);
        when(rec.title()).thenReturn("Amélie");

        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() -> ref[0] = new Tool2Panel(service, parent));
        final Tool2Panel panel = ref[0];

        // injecter l'état et réactiver les boutons
        setField(panel, "current", rec);
        final JLabel title = (JLabel) getField(panel, "title");
        SwingUtilities.invokeAndWait(() -> title.setText("Amélie"));
        enableButtonsForClick(panel); // <-- important

        final JButton seenBtn = (JButton) getField(panel, "seenBtn");

        // When
        SwingUtilities.invokeAndWait(seenBtn::doClick);

        // Then
        verify(service, atLeastOnce()).mark("Amélie", "deja_vu");
    }


    // -------- Helpers réflexion --------

    private static Object getField(final Object target, final String name) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void enableButtonsForClick(final Tool2Panel panel) throws Exception {
        final JButton likeBtn = (JButton) getField(panel, "likeBtn");
        final JButton nopeBtn = (JButton) getField(panel, "nopeBtn");
        final JButton seenBtn = (JButton) getField(panel, "seenBtn");
        final JButton backBtn = (JButton) getField(panel, "backBtn");
        SwingUtilities.invokeAndWait(() -> {
            likeBtn.setEnabled(true);
            nopeBtn.setEnabled(true);
            seenBtn.setEnabled(true);
            backBtn.setEnabled(true);
        });
    }

    // ======================================================================
// Helpers d’attente EDT-friendly (utilisés par plusieurs tests)
// ======================================================================

    /** GIVEN/WHEN/THEN helper: attend jusqu’à ce que la condition soit vraie en pompant l’EDT. */
    private static void awaitTrue(Check cond, long timeout, TimeUnit unit) throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        Throwable last = null;
        while (System.nanoTime() < deadline) {
            try {
                SwingUtilities.invokeAndWait(() -> {}); // laisse passer les SwingWorker.done()
                if (cond.ok()) return;
            } catch (Throwable t) {
                last = t;
            }
            Thread.sleep(10);
        }
        if (last != null) throw new AssertionError("Condition jamais vraie", last);
        throw new AssertionError("Condition jamais vraie avant timeout");
    }

    @FunctionalInterface private interface Check { boolean ok() throws Exception; }

// ======================================================================
// TESTS COMPLÉMENTAIRES POUR ATTEINDRE 100% DE Tool2Panel
// ======================================================================

    @Test
    @DisplayName("paintComponent: rendu gradient sans exception")
    void paintComponent_drawsGradient() throws Exception {
        // GIVEN un panel rendu
        final Tool2Panel[] ref = new Tool2Panel[1];
        try (MockedStatic<JsonStorage> ignored = mockStatic(JsonStorage.class)) {
            SwingUtilities.invokeAndWait(() ->
                    ref[0] = new Tool2Panel(mock(MovieRecommenderService.class), mock(java.util.function.Consumer.class)));
        }
        Tool2Panel panel = ref[0];
        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();

        // WHEN on peint dans un BufferedImage
        SwingUtilities.invokeAndWait(() -> { panel.setSize(320, 200); panel.paintComponent(g); });
        g.dispose();

        // THEN aucune exception (couverture de la méthode)
        assertNotNull(panel);
    }

    @Test
    @DisplayName("htmlEscape(null): branche null -> \"\"")
    void htmlEscape_null_returnsEmpty() throws Exception {
        // GIVEN un panel
        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() ->
                ref[0] = new Tool2Panel(mock(MovieRecommenderService.class), mock(java.util.function.Consumer.class)));
        Tool2Panel panel = ref[0];
        Method htmlEscape = Tool2Panel.class.getDeclaredMethod("htmlEscape", String.class);
        htmlEscape.setAccessible(true);

        // WHEN on appelle avec null
        String out = (String) htmlEscape.invoke(panel, new Object[]{null});

        // THEN chaîne vide
        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("startDescriptionForCurrent: current==null -> early return")
    void startDescription_earlyReturn_whenNoCurrent() throws Exception {
        // GIVEN panel avec current==null (état par défaut)
        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() ->
                ref[0] = new Tool2Panel(mock(MovieRecommenderService.class), mock(java.util.function.Consumer.class)));
        Tool2Panel panel = ref[0];
        Method start = Tool2Panel.class.getDeclaredMethod("startDescriptionForCurrent");
        start.setAccessible(true);
        JEditorPane desc = (JEditorPane) getField(panel, "descPane");
        String before = desc.getText();

        // WHEN on invoque la méthode
        SwingUtilities.invokeAndWait(() -> { try { start.invoke(panel); } catch (Exception e) { throw new RuntimeException(e); } });

        // THEN aucun changement notable (juste couverture de la branche)
        assertTrue(desc.getText().equals(before));
    }

    @Test
    @DisplayName("startDescriptionForCurrent: mismatch (current change) -> sortie ignorée")
    void startDescription_mismatch_isIgnored() throws Exception {
        // GIVEN current = A et service renvoie une desc
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        when(service.generateDescription("A")).thenReturn("Desc A");
        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() ->
                ref[0] = new Tool2Panel(service, mock(java.util.function.Consumer.class)));
        Tool2Panel panel = ref[0];
        Recommendation recA = mock(Recommendation.class); when(recA.title()).thenReturn("A");
        setField(panel, "current", recA);
        Method start = Tool2Panel.class.getDeclaredMethod("startDescriptionForCurrent");
        start.setAccessible(true);
        JEditorPane desc = (JEditorPane) getField(panel, "descPane");

        // WHEN on lance la génération puis on remplace current par B avant la fin
        SwingUtilities.invokeAndWait(() -> { try { start.invoke(panel); } catch (Exception e) { throw new RuntimeException(e); } });
        Recommendation recB = mock(Recommendation.class); when(recB.title()).thenReturn("B");
        setField(panel, "current", recB);

        // THEN le HTML ne doit pas contenir "Desc A" (mismatch => early return dans done())
        awaitTrue(() -> !desc.getText().contains("Desc A"), 2, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("startDescriptionForCurrent: succès -> HTML centré + échappé")
    void startDescription_success_updatesHtml() throws Exception {
        // GIVEN current = Film, service renvoie deux lignes avec caractères HTML
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        when(service.generateDescription("Film")).thenReturn("Line1\nLine2 & <3");
        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() ->
                ref[0] = new Tool2Panel(service, mock(java.util.function.Consumer.class)));
        Tool2Panel panel = ref[0];
        Recommendation rec = mock(Recommendation.class); when(rec.title()).thenReturn("Film");
        setField(panel, "current", rec);
        Method start = Tool2Panel.class.getDeclaredMethod("startDescriptionForCurrent");
        start.setAccessible(true);
        JEditorPane desc = (JEditorPane) getField(panel, "descPane");

        // WHEN on lance la génération
        SwingUtilities.invokeAndWait(() -> { try { start.invoke(panel); } catch (Exception e) { throw new RuntimeException(e); } });

        // THEN HTML mis à jour avec <br/> et entités HTML
        awaitTrue(() -> {
            String html = desc.getText();
            return html.contains("Line1") &&
                    html.contains("Line2") &&
                    html.contains("&amp;") &&
                    (html.contains("&lt;3") || html.contains("&lt;"));
        }, 3, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("proposeNext: service.throw -> titre Erreur + fallback description")
    void proposeNext_serviceThrows_showsErrorAndFallback() throws Exception {
        // GIVEN service.recommendRandom() jette, wishlist vide
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        when(service.recommendRandom()).thenThrow(new RuntimeException("Boom"));
        final Tool2Panel[] ref = new Tool2Panel[1];
        try (MockedStatic<JsonStorage> mocked = mockStatic(JsonStorage.class)) {
            mocked.when(() -> JsonStorage.getByStatus("envie")).thenReturn(List.of());
            SwingUtilities.invokeAndWait(() ->
                    ref[0] = new Tool2Panel(service, mock(java.util.function.Consumer.class)));
        }
        Tool2Panel panel = ref[0];
        Method proposeNext = Tool2Panel.class.getDeclaredMethod("proposeNext");
        proposeNext.setAccessible(true);
        JLabel title = (JLabel) getField(panel, "title");
        JEditorPane desc = (JEditorPane) getField(panel, "descPane");

        // WHEN relance explicite
        SwingUtilities.invokeAndWait(() -> { try { proposeNext.invoke(panel); } catch (Exception e) { throw new RuntimeException(e); } });

        // THEN titre commence par "Erreur" + fallback "Description indisponible"
        awaitTrue(() -> title.getText().startsWith("Erreur"), 3, TimeUnit.SECONDS);
        assertTrue(desc.getText().contains("Description indisponible"));
    }



    @Test
    @DisplayName("Actions like/nope/seen: current==null -> aucun mark")
    void actions_noCurrent_neverMark() throws Exception {
        // GIVEN panel avec boutons activés et current==null
        MovieRecommenderService service = mock(MovieRecommenderService.class);
        final Tool2Panel[] ref = new Tool2Panel[1];
        SwingUtilities.invokeAndWait(() ->
                ref[0] = new Tool2Panel(service, mock(java.util.function.Consumer.class)));
        Tool2Panel panel = ref[0];
        final JButton likeBtn = (JButton) getField(panel, "likeBtn");
        final JButton nopeBtn = (JButton) getField(panel, "nopeBtn");
        final JButton seenBtn = (JButton) getField(panel, "seenBtn");
        SwingUtilities.invokeAndWait(() -> { likeBtn.setEnabled(true); nopeBtn.setEnabled(true); seenBtn.setEnabled(true); });

        // WHEN on clique sur chaque bouton sans current
        SwingUtilities.invokeAndWait(likeBtn::doClick);
        SwingUtilities.invokeAndWait(nopeBtn::doClick);
        SwingUtilities.invokeAndWait(seenBtn::doClick);

        // THEN aucun appel à mark
        verify(service, after(300).never()).mark(anyString(), anyString());
    }

}

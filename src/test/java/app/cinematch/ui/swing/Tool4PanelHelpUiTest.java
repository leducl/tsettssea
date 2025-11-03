package app.cinematch.ui.swing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Couverture UI de l'aide (showHelpDialog + A-/Défaut/A+/Fermer) et buildHelpHtml(double).
 * GIVEN / WHEN / THEN au début des blocs uniquement.
 */
class Tool4PanelHelpUiTest {

    // Helpers réflexion & recherche de composants
    @SuppressWarnings("unchecked")
    private static <T> T getPrivate(Object target, String field) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Impossible d'accéder à " + field, e);
        }
    }

    private static double getHelpScale(Tool4Panel panel) {
        try {
            Field f = Tool4Panel.class.getDeclaredField("helpScale");
            f.setAccessible(true);
            return f.getDouble(panel);
        } catch (Exception e) {
            throw new AssertionError("Impossible de lire helpScale", e);
        }
    }

    private static <T extends Component> T findIn(Container root, Class<T> type, Predicate<T> pred) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && pred.test(type.cast(c))) {
                return type.cast(c);
            }
            if (c instanceof Container cont) {
                T r = findIn(cont, type, pred);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JDialog waitHelpDialog() throws Exception {
        for (int i = 0; i < 100; i++) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isShowing()
                        && "Aide — Agent IA".equals(d.getTitle())) {
                    return d;
                }
            }
            Thread.sleep(20);
        }
        fail("Dialog d'aide introuvable");
        return null;
    }

    @Test
    @DisplayName("Aide: clic sur le bouton ? -> dialog ouverte, zoom A+/A-/Défaut, puis Fermer")
    void givenInfoButton_whenClicked_thenDialogOpensAndZoomButtonsWork() throws Exception {
        // GIVEN
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Test UI ignoré en mode headless");
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        JButton info = getPrivate(panel, "infoButton");
        double initial = getHelpScale(panel);
        assertEquals(0.9d, initial, 1e-9);

        // WHEN
        SwingUtilities.invokeAndWait(info::doClick);
        JDialog dlg = waitHelpDialog();

        // THEN
        assertTrue(dlg.isShowing());
        JEditorPane editor = findIn(dlg.getContentPane(), JEditorPane.class, e -> true);
        JButton zoomOut = findIn(dlg.getContentPane(), JButton.class, b -> "A-".equals(b.getText()));
        JButton reset   = findIn(dlg.getContentPane(), JButton.class, b -> "Défaut".equals(b.getText()));
        JButton zoomIn  = findIn(dlg.getContentPane(), JButton.class, b -> "A+".equals(b.getText()));
        JButton close   = findIn(dlg.getContentPane(), JButton.class, b -> "Fermer".equals(b.getText()));
        assertNotNull(editor);
        assertNotNull(zoomOut);
        assertNotNull(reset);
        assertNotNull(zoomIn);
        assertNotNull(close);

        // WHEN
        SwingUtilities.invokeAndWait(zoomIn::doClick);

        // THEN
        assertEquals(1.0d, getHelpScale(panel), 1e-9);

        // WHEN
        SwingUtilities.invokeAndWait(zoomOut::doClick);

        // THEN
        assertEquals(0.9d, getHelpScale(panel), 1e-9);

        // WHEN
        SwingUtilities.invokeAndWait(reset::doClick);

        // THEN
        assertEquals(0.9d, getHelpScale(panel), 1e-9);

        // WHEN
        SwingUtilities.invokeAndWait(close::doClick);

        // THEN
        for (int i = 0; i < 50; i++) {
            if (!dlg.isDisplayable() || !dlg.isShowing()) break;
            Thread.sleep(10);
        }
        assertFalse(dlg.isShowing());
    }

    @Test
    @DisplayName("Aide: raccourci F1 (Action 'showHelp') -> dialog ouverte")
    void givenF1Action_whenInvoked_thenDialogOpens() throws Exception {
        // GIVEN
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Test UI ignoré en mode headless");
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        Action action = panel.getActionMap().get("showHelp");
        assertNotNull(action);

        // WHEN
        SwingUtilities.invokeAndWait(() ->
                action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "showHelp"))
        );

        // THEN
        JDialog dlg = waitHelpDialog();
        assertTrue(dlg.isShowing());
        SwingUtilities.invokeAndWait(dlg::dispose);
    }

    @Test
    @DisplayName("buildHelpHtml(double) & buildHelpHtml(): remplacements des tokens et tailles cohérentes")
    void givenScales_whenBuildHelpHtml_thenTokensAreReplaced() throws Exception {
        // GIVEN
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        Method mScaled = Tool4Panel.class.getDeclaredMethod("buildHelpHtml", double.class);
        mScaled.setAccessible(true);
        Method mDefault = Tool4Panel.class.getDeclaredMethod("buildHelpHtml");
        mDefault.setAccessible(true);

        // WHEN
        String minHtml = (String) mScaled.invoke(panel, 0.9d);
        String maxHtml = (String) mScaled.invoke(panel, 2.0d);
        String defHtml = (String) mDefault.invoke(panel);

        // THEN
        assertTrue(minHtml.contains("font-size:14px"));
        assertTrue(maxHtml.contains("font-size:32px"));
        assertTrue(defHtml.contains("font-size:14px"));
        assertFalse(minHtml.contains("{PAD}"));
        assertFalse(minHtml.contains("{BODY}"));
    }
}

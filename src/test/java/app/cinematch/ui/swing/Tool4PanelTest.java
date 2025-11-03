package app.cinematch.ui.swing;

import app.cinematch.agent.ChatAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du nouveau Tool4Panel.
 * Style "given / when / then" avec couverture complète.
 */
class Tool4PanelTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    // --- OUTILS REFLEXION ----------------------------------------------------

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

    @SuppressWarnings("SameParameterValue")
    private static Object callPrivate(Object target, String methodName, Class<?>[] sig, Object... args) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, sig);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception e) {
            throw new AssertionError("Impossible d’appeler " + methodName, e);
        }
    }


    private static void onEDTAndWait(Runnable r) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeAndWait(r);
    }

    @SuppressWarnings({"BusyWait", "SameParameterValue"})
    private static void waitUntil(String msg, long timeoutMs, Condition cond) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (cond.ok()) return;
            Thread.sleep(10);
        }
        fail("Timeout: " + msg);
    }

    @FunctionalInterface
    private interface Condition { boolean ok(); }

    // --- TESTS ---------------------------------------------------------------

    @Test
    void givenAskThrows_whenSend_thenErrorAppearsAndLoaderStops() throws Exception {
        Function<String, String> failing = s -> { throw new RuntimeException("Boom"); };
        Tool4Panel panel = new Tool4Panel(failing, s -> {});
        JTextField input = getPrivate(panel, "inputField");
        JButton send = getPrivate(panel, "sendButton");
        JTextPane pane = getPrivate(panel, "conversationPane");

        onEDTAndWait(() -> input.setText("Test erreur"));
        onEDTAndWait(send::doClick);

        waitUntil("message d'erreur visible", 2000, () ->
                pane.getText().contains("Erreur") && pane.getText().contains("Boom"));
    }

    @Test
    void givenEmptyInput_whenSend_thenNothingChanges() throws Exception {
        Function<String, String> askFn = s -> "Ne devrait pas être appelé";
        Tool4Panel panel = new Tool4Panel(askFn, s -> {});
        JTextField input = getPrivate(panel, "inputField");
        JButton send = getPrivate(panel, "sendButton");
        JTextPane pane = getPrivate(panel, "conversationPane");

        onEDTAndWait(() -> input.setText("   "));
        onEDTAndWait(send::doClick);

        assertTrue(pane.getText().contains("Bienvenue"), "Le message d'accueil doit rester inchangé");
        assertTrue(send.isEnabled());
    }

    @Test
    void givenBackButton_whenClicked_thenNavigatorIsCalled() throws Exception {
        AtomicReference<String> nav = new AtomicReference<>();
        Tool4Panel panel = new Tool4Panel(s -> "ok", nav::set);
        JButton back = getPrivate(panel, "backButton");

        onEDTAndWait(back::doClick);
        assertEquals("home", nav.get(), "Le bouton retour doit appeler navigator.accept(\"home\")");
    }

    @Test
    void givenButtons_whenHover_thenColorsChange() {
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        JButton send = getPrivate(panel, "sendButton");
        JButton back = getPrivate(panel, "backButton");

        Color before = send.getBackground();
        MouseEvent enterSend = new MouseEvent(send, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : send.getMouseListeners()) l.mouseEntered(enterSend);
        Color afterEnter = send.getBackground();
        assertNotEquals(before, afterEnter);

        MouseEvent exitSend = new MouseEvent(send, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : send.getMouseListeners()) l.mouseExited(exitSend);
        Color afterExit = send.getBackground();
        assertEquals(new Color(50, 40, 60), afterExit);

        Color fgBefore = back.getForeground();
        MouseEvent enterBack = new MouseEvent(back, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : back.getMouseListeners()) l.mouseEntered(enterBack);
        Color fgEnter = back.getForeground();
        assertNotEquals(fgBefore, fgEnter);

        MouseEvent exitBack = new MouseEvent(back, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 5, 5, 0, false);
        for (var l : back.getMouseListeners()) l.mouseExited(exitBack);
        Color fgExit = back.getForeground();
        assertEquals(new Color(220, 220, 220), fgExit);
    }

    @Test
    void givenPanel_whenPaintComponent_thenGradientDrawsProperly() {
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        panel.setSize(300, 150);

        BufferedImage img = new BufferedImage(300, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g2));
        g2.dispose();

        int argb = img.getRGB(150, 75);
        assertTrue((argb >>> 24) > 0, "Le fond dégradé doit être peint");
    }

    @Test
    void givenPrivateCompound_whenCalledByReflection_thenReturnsCompoundBorder() {
        Tool4Panel panel = new Tool4Panel(s -> "ok", s -> {});
        Object border = callPrivate(panel, "compound",
                new Class[]{Color.class, int.class, EmptyBorder.class},
                Color.WHITE, 2, new EmptyBorder(1, 1, 1, 1));

        assertInstanceOf(CompoundBorder.class, border);
    }

    @Test
    void givenSecondConstructorWithChatAgent_whenSend_thenUsesAgentAsk() throws Exception {
        ChatAgent agent = mock(ChatAgent.class);
        when(agent.ask("Ping")).thenReturn("Pong");
        Tool4Panel panel = new Tool4Panel(agent, s -> {});
        JTextField input = getPrivate(panel, "inputField");
        JButton send = getPrivate(panel, "sendButton");
        JTextPane pane = getPrivate(panel, "conversationPane");

        onEDTAndWait(() -> input.setText("Ping"));
        onEDTAndWait(send::doClick);

        waitUntil("réponse IA visible", 2000, () -> pane.getText().contains("Pong"));
        verify(agent).ask("Ping");
    }
}

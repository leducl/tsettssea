package app.cinematch.ui.swing;

import javax.swing.*;

public class MainFrameStub extends JFrame {
    public String lastShownCard;
    public MainFrameStub(Object o) {}
    public void showCard(String name) { this.lastShownCard = name; }

}

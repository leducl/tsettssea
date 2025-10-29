package app.cinematch.ui.swing;

import javax.swing.*;

public class MainFrame extends JFrame {
    public String lastShownCard;
    public MainFrame(Object o) {}
    public void showCard(String name) { this.lastShownCard = name; }
}

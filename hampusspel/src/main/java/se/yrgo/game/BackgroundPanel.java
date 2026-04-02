package se.yrgo.game;

import javax.swing.*;
import java.awt.*;

public class BackgroundPanel extends JPanel {
    private Image background = Toolkit.getDefaultToolkit().createImage("background.jpg");

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        super.paintComponent(g);
    }

}

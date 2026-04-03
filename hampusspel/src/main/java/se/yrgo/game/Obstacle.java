package se.yrgo.game;

import java.awt.Rectangle;

// this class can be much improved, better encapsulation
// draw itself, update itself etc. etc.
public class Obstacle {
    public final int timeCreated;
    public final Rectangle bounds;

    public Obstacle(int created, int x, int y) {
        this.timeCreated = created;
        this.bounds = new Rectangle(x, y, 150, 600);
    }
}
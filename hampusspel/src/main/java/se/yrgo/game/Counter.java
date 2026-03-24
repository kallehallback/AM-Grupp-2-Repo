package se.yrgo.game;

import java.awt.Rectangle;

// this class can be much improved, better encapsulation
// draw itself, update itself etc. etc.
public class Counter {
    public final int timeCreated;
    public final Rectangle bounds;

    public Counter(int created, int x) {
        this.timeCreated = created;
        this.bounds = new Rectangle(x + 500, 200, 1, 900);
    }
}

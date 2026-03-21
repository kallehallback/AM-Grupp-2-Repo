package se.yrgo.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * A simple panel with a space invaders "game" in it. This is just to
 * demonstrate the bare minimum of stuff than can be done drawing on a panel.
 * This is by no means good code, but rather a short demonstration on
 * some things one can do to make a very simple Swing based game.
 * 
 * If you really want to make a good game there are several toolkits for
 * game making out there which are much more suitable for this.
 * 
 */
public class GameSurface extends JPanel implements KeyListener {
    private static final long serialVersionUID = 6260582674762246325L;
    private static Logger logger = Logger.getLogger(GameSurface.class.getName());

    private static final double PIPE_PIXELS_PER_MS = 0.25;
    private static final double SCORE_PER_SECOND = 0.5;

    // make some transient to get past boring serialization demands...
    private transient FrameUpdater updater;
    private boolean gameOver;
    private transient List<Pipe> pipes;
    private Rectangle birb;
    private transient BufferedImage birbImageSprite;
    private int birbImageSpriteCount;

    private int score;

    private double velocity = 0;
    private double gravity = 0.2;

    private int frameWidth = 17;
    private int frameHeight = 12;
    private int scale = 5;
    private int drawWidth = frameWidth * scale; // 85
    private int drawHeight = frameHeight * scale; // 60
    private int offset = frameWidth * birbImageSpriteCount;

    private long lastPipeSpawnTime = 0;
    private static final int PIPE_SPAWN_INTERVAL = 2000;

    public GameSurface(final int width) {
        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/birb.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /birb.png");
            } else {
                this.birbImageSprite = ImageIO.read(spriteStream);
            }
            this.birbImageSpriteCount = 0;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /birb.png", ex);
        }

        this.gameOver = false;
        this.pipes = new ArrayList<>();
        this.birb = new Rectangle(500, 432, 85, 60);
        this.score = 0;

        this.updater = new FrameUpdater(this, 60);
        this.updater.setDaemon(true); // it should not keep the app running
        this.updater.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        drawSurface(g2d);
    }

    /**
     * Call this method when the graphics needs to be repainted on the graphics
     * surface.
     * 
     * @param g the graphics to paint on
     */
    private void drawSurface(Graphics2D g) {
        final Dimension d = this.getSize();

        if (gameOver) {
            g.setColor(Color.red);
            g.fillRect(0, 0, d.width, d.height);
            g.setColor(Color.black);
            g.setFont(new Font("Arial", Font.BOLD, 100));
            g.drawString("Game over!", 475, 432);
            drawScore(g, d, true);
            return;
        }

        // fill the background
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, d.width, d.height);

        // draw the pipe
        for (Pipe pipe : pipes) {
            g.setColor(Color.GREEN);
            g.fillRect(pipe.bounds.x, pipe.bounds.y, pipe.bounds.width, pipe.bounds.height);

            // draw the outline
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(5)); // thickness
            g.drawRect(pipe.bounds.x, pipe.bounds.y, pipe.bounds.width, pipe.bounds.height);
        }

        // draw the bird
        g.drawImage(
                birbImageSprite,
                birb.x,
                birb.y,
                birb.x + drawWidth,
                birb.y + drawHeight,
                offset,
                0,
                offset + frameWidth,
                frameHeight,
                null);

        // draw the score
        drawScore(g, d, false);
    }

    private void drawScore(Graphics2D g, Dimension d, boolean gameOverBackground) {
        final String scoreText = String.valueOf(score);
        final Font scoreFont = new Font("Monospaced", Font.BOLD, 100);

        g.setFont(scoreFont);
        FontMetrics metrics = g.getFontMetrics(scoreFont);
        int x = d.width - metrics.stringWidth(scoreText) - 733; // x position
        int y = 10 + metrics.getAscent(); // y position

        // text outline
        g.setColor(Color.BLACK);
        g.drawString(scoreText, x - 2, y);
        g.drawString(scoreText, x + 2, y);
        g.drawString(scoreText, x, y - 2);
        g.drawString(scoreText, x, y + 2);

        // text
        g.setColor(Color.WHITE);
        g.drawString(scoreText, x, y);
    }

    public void update(int time) {
        if (gameOver) {
            updater.interrupt();
            return;
        }

        velocity += gravity;
        birb.y += velocity;

        final Dimension d = getSize();
        if (d.height <= 0 || d.width <= 0) {
            // if the panel has not been placed properly in the frame yet
            // just return without updating any state
            return;
        }

        // spawns a pipe at the start of the game
        if (lastPipeSpawnTime == 0) {
            lastPipeSpawnTime = time - PIPE_SPAWN_INTERVAL;
        }

        // contineusly spawn pipes every other second
        if (time - lastPipeSpawnTime >= PIPE_SPAWN_INTERVAL) {
            addPipe(time, d.height);
            lastPipeSpawnTime = time;
        }

        // add 1 point every other second, after a 2 second delay
        score = (int) (((time - 2200) / 1000.0) * SCORE_PER_SECOND);

        final List<Pipe> toRemove = new ArrayList<>();

        for (Pipe pipe : pipes) {
            // movement is based on elapsed time to make it smoother and
            // more consistent over different computers
            int timeElapsed = time - pipe.timeCreated;
            pipe.bounds.x = (int) (d.width - (timeElapsed * PIPE_PIXELS_PER_MS));
            if (pipe.bounds.x + pipe.bounds.width < 0) {
                // we add to another list and remove later
                // to avoid concurrent modification in a for-each loop
                toRemove.add(pipe);
            }

            if (pipe.bounds.intersects(birb)) {
                gameOver = true;
            }
        }

        // remove all pipes that are out of frame
        pipes.removeAll(toRemove);
    }

    private void addPipe(final int time, final int height) {
        int newTime = time;
        final int FAR_OFFSCREEN = 10000;

        // the position of the upper pipe
        int y1 = ThreadLocalRandom.current().nextInt(-400, height - 900);
        pipes.add(new Pipe(newTime, FAR_OFFSCREEN, y1));

        // and the lower one
        int y2 = y1 + 800;
        pipes.add(new Pipe(newTime, FAR_OFFSCREEN, y2));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // this event triggers when we release a key and then
        // we will move the space ship if the game is not over yet

        if (gameOver) {
            return;
        }

        final int kc = e.getKeyCode();

        if (kc == KeyEvent.VK_SPACE) {
            velocity = -6;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // do nothing
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // do nothing
    }
}
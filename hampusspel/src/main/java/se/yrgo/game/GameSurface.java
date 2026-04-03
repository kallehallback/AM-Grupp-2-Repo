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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
public class GameSurface extends JPanel implements KeyListener, MouseListener {
    private static final long serialVersionUID = 6260582674762246325L;
    private static Logger logger = Logger.getLogger(GameSurface.class.getName());

    private static final double OBSTACLE_PIXELS_PER_MS = 0.25;

    // make some transient to get past boring serialization demands...
    private transient FrameUpdater updater;
    private boolean gameOver;
    private transient List<Obstacle> obstacles;
    private transient List<Counter> counters;
    private Rectangle player;
    private transient BufferedImage playerImageSprite;
    private int playerImageSpriteCount;

    private int score;

    private double jumpHeight = 0;
    private double gravity = 0.3;

    private int playerWidth = 17;
    private int playerHeight = 12;
    private int scale = 5;
    private int drawWidth = playerWidth * scale; // 85
    private int drawHeight = playerHeight * scale; // 60
    private int offset = playerWidth * playerImageSpriteCount;

    private long lastObstacleSpawnTime = 0;
    private static final int OBSTACLE_SPAWN_INTERVAL = 2500;

    public GameSurface(final int width) {

        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/birb.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /birb.png");
            } else {
                this.playerImageSprite = ImageIO.read(spriteStream);
            }
            this.playerImageSpriteCount = 0;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /birb.png", ex);
        }

        this.gameOver = false;
        this.obstacles = new ArrayList<>();
        this.counters = new ArrayList<>();
        this.player = new Rectangle(500, 432, 85, 60);
        this.score = 0;

        this.addMouseListener(this);

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
        for (Obstacle obstacle : obstacles) {
            drawObstacle(g, obstacle);
        }

        // draw the bird
        g.drawImage(
                playerImageSprite,
                player.x,
                player.y,
                player.x + drawWidth,
                player.y + drawHeight,
                offset,
                0,
                offset + playerWidth,
                playerHeight,
                null);

        // draw the score
        drawScore(g, d, false);
    }

    private void drawObstacle(Graphics2D g, Obstacle obstacle) {
        g.setColor(Color.GREEN);
        g.fillRect(obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height);

        // draw the outline
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(5)); // thickness
        g.drawRect(obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height);
    }

    private void drawScore(Graphics2D g, Dimension d, boolean gameOverBackground) {
        final String scoreText = String.valueOf(score / 20);
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

        jumpHeight += gravity;
        player.y += jumpHeight;

        if (player.y < 0)
            player.y = 0;
        else if (player.y > 850) {
            gameOver = true;
        }

        final Dimension d = getSize();
        if (d.height <= 0 || d.width <= 0) {
            // if the panel has not been placed properly in the frame yet
            // just return without updating any state
            return;
        }

        // spawns a pipe at the start of the game
        if (lastObstacleSpawnTime == 0) {
            lastObstacleSpawnTime = time - OBSTACLE_SPAWN_INTERVAL;
        }

        // contineusly spawn pipes every other second
        if (time - lastObstacleSpawnTime >= OBSTACLE_SPAWN_INTERVAL) {
            addObstacle(time, d.height);
            addCounter(time);
            lastObstacleSpawnTime = time;
        }

        manageObstacles(time, d);
        manageCounters(time, d);
    }

    private void manageObstacles(int time, final Dimension d) {
        final List<Obstacle> toRemoveObstacle = new ArrayList<>();
        for (Obstacle obstacle : obstacles) {
            int timeElapsed = time - obstacle.timeCreated;
            obstacle.bounds.x = (int) (d.width - (timeElapsed * OBSTACLE_PIXELS_PER_MS));
            if (obstacle.bounds.x + obstacle.bounds.width < 0) {
                toRemoveObstacle.add(obstacle);
            }

            if (obstacle.bounds.intersects(player)) {
                gameOver = true;
            }
        }
        obstacles.removeAll(toRemoveObstacle);
    }

    private void manageCounters(int time, final Dimension d) {
        final List<Counter> toRemoveCounter = new ArrayList<>();
        for (Counter counter : counters) {
            int timeElapsed = time - counter.timeCreated;
            counter.bounds.x = (int) (d.width - (timeElapsed * OBSTACLE_PIXELS_PER_MS) + 150);
            if (counter.bounds.x + counter.bounds.width < 0) {
                toRemoveCounter.add(counter);
            }

            if (counter.bounds.intersects(player)) {
                score = score + 1;
            }
        }
        counters.removeAll(toRemoveCounter);
    }

    private void addObstacle(final int time, final int height) {
        int newTime = time;
        final int FAR_OFFSCREEN = 9000;

        // the position of the upper pipe
        int y1 = ThreadLocalRandom.current().nextInt(-400, height - 900);
        obstacles.add(new Obstacle(newTime, FAR_OFFSCREEN, y1));

        // and the lower one
        int y2 = y1 + 800;
        obstacles.add(new Obstacle(newTime, FAR_OFFSCREEN, y2));
    }

    private void addCounter(final int time) {
        int newTime = time;
        final int FAR_OFFSCREEN = 9300;
        counters.add(new Counter(newTime, FAR_OFFSCREEN));
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
            jumpHeight = -7;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

        if (gameOver) {
            return;
        }

        final int b = e.getButton();

        if (b == MouseEvent.BUTTON1) {
            jumpHeight = -7;
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

    @Override
    public void mouseClicked(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // do nothing
    }
}
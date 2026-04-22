package se.yrgo.game;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;

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
    private Highscore archive = new Highscore();
    private SoundPlayer sound = new SoundPlayer();
    private ArrayList<Player> highscore = new ArrayList<>();
    private static final long serialVersionUID = 6260582674762246325L;
    private static Logger logger = Logger.getLogger(GameSurface.class.getName());

    private static final double OBSTACLE_PIXELS_PER_MS = 0.25;

    // make some transient to get past boring serialization demands...
    private transient FrameUpdater updater;
    private boolean gameOver;
    private boolean gameStarted;
    private boolean once = true;
    private boolean showMenu = true;
    private transient List<Obstacle> obstacles;
    private transient List<Counter> counters;
    private Rectangle player;
    private transient BufferedImage playerImageSprite;
    private transient BufferedImage obstacleImageSprite;
    private int playerImageSpriteCount;
    private BufferedImage background;
    private BufferedImage nameBackground;
    private BufferedImage gameOverBackground;
    private boolean inputname=false;
    private String playerName = "";
    private File highScoreFile = new File("src\\main\\resources\\highscore.txt");

    private int score;

    private double jumpHeight = 0;
    private double gravity = 0.3;

    private int playerWidth = 85;
    private int playerHeight = 85;

    private long lastObstacleSpawnTime = 0;
    private static final int OBSTACLE_SPAWN_INTERVAL = 2500;

    public GameSurface(final int width) {
        try{
            highScoreFile.createNewFile();

        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to read or access high score");
        }
        highscore = archive.loadScore("highscore.txt");
        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/witch.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /witch.png");
            } else {
                this.playerImageSprite = ImageIO.read(spriteStream);
            }
            this.playerImageSpriteCount = 0;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /birb.png", ex);
        }
        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/tree.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /tree.png");
            } else {
                this.obstacleImageSprite = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /tree.png", ex);
        }

        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/forest.jpg")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /background.jpg");
            } else {
                this.background = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /background.jpg", ex);
        }

        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/witch_name.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /witch_name.png");
            } else {
                this.nameBackground = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /witch_name.png", ex);
        }

        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/gameover.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /gameover.png");
            } else {
                this.gameOverBackground = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /gameover.png", ex);
        }

        this.gameOver = false;
        this.gameStarted = false;
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


        if (showMenu) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, d.width, d.height);

            g.setColor(Color.WHITE);
            g.fillRect(400, 200, 500, 150);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 80));
            g.drawString("jumpy Witch", 420, 300);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Press SPACE to Start", 450, 550);

            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.WHITE);
            int newLine = g.getFont().getSize() + 5;
            int y = 100;
            g.drawString("High Score:", 25, y);
            for(int i=0;i<highscore.size();i++){
                if(i<10) {
                    Player player = highscore.get(i);
                    g.drawString((i + 1) + " - " + player.name + " : " + player.score, 25, y += newLine);
                }
            }
            return;
        }

        if(!showMenu && !gameStarted){
            g.drawImage(nameBackground, 0, 0, null);
            g.drawImage(nameBackground, 1472, 0, null);
            g.setFont(new Font("Old English Text MT", Font.BOLD, 24));
            g.drawString("Please enter your name:", 710, 300);
            g.setFont(new Font("Ink Free", Font.BOLD, 20));
            g.drawString(playerName, 710, 350);
            super.repaint();
            return;
        }

        if (gameOver) {
            if (once) {
                Player player1 = new Player(playerName, score / 20);
                highscore.add(player1);
                Comparator myComparator = new SortByScore();
                Collections.sort(highscore, myComparator);
                archive.saveScore(highscore, "highscore.txt");
                once = false;
            }
            g.drawImage(gameOverBackground, 0, 0, null);
            g.drawImage(gameOverBackground, 1472, 0, null);
            g.setColor(Color.white);
            g.setFont(new Font("Old English Text MT", Font.PLAIN, 100));

            g.drawString("Game over!", 475, 150);
            g.setFont(new Font("Old English Text MT", Font.PLAIN, 50));
            g.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
            int y= 220;
            int newLine = g.getFont().getSize() + 10;
            g.drawString("High Score:", 595, y);
            for(int i=0;i<highscore.size();i++){
                if(i<10) {
                    Player player = highscore.get(i);
                    g.drawString((i + 1) + " - " + player.name + " : " + player.score, 550, y += newLine);
                }
            }
            return;
        }




        g.drawImage(background, 0, 0, null);
        g.drawImage(background, 1472, 0, null);

        // draw the pipe
        for (Obstacle obstacle : obstacles) {
            drawObstacle(g, obstacle);
        }

        // draw the bird
        if (playerImageSprite != null) {
            int offset = 85 * playerImageSpriteCount;
            g.drawImage(
                    playerImageSprite,
                    player.x,
                    player.y,
                    player.x + playerWidth,
                    player.y + playerHeight,
                    offset,
                    0,
                    offset + playerWidth, playerHeight,
                    null);
        }

        // draw the score
        drawScore(g, d, false);


    }
    private void drawObstacle(Graphics2D g, Obstacle obstacle) {
        g.drawImage(obstacleImageSprite,
                obstacle.bounds.x,
                obstacle.bounds.y,
                obstacle.bounds.width,
                obstacle.bounds.height,
                null);
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

        if (!gameStarted) {
            return;
        }

        jumpHeight += gravity;
        player.y += jumpHeight;

        if (player.y < 0)
            player.y = 0;
        else if (player.y > 850) {
            gameOver = true;
            sound.playSound("/witchlaugh.wav");
        }

        final Dimension d = getSize();
        if (d.height <= 0 || d.width <= 0) {
            // if the panel has not been placed properly in the frame yet
            // just return without updating any state
            return;
        }

        playerImageSpriteCount = (time / 100) % 3;

        // spawns a pipe at the start of the game
        if (lastObstacleSpawnTime == 0) {
            lastObstacleSpawnTime = time - OBSTACLE_SPAWN_INTERVAL;
        }

        // contineusly spawn pipes every 2.5 seconds
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
                sound.playSound("/witchlaugh.wav");
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
                score++;
            }

            if (counter.bounds.x <= player.x && !counter.counted) {
                sound.playSound("/score.wav");
                counter.counted = true;
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

    private void restartGame() {
        Dimension d = getSize();

        gameOver = false;
        once = true;
        obstacles.clear();
        counters.clear();

        addObstacle(0, d.height);
        addCounter(0);

        player = new Rectangle(500, 432, 85, 60);

        jumpHeight = 0;
        score = 0;
        lastObstacleSpawnTime = -1;

        updater = new FrameUpdater(this, 60);
        updater.setDaemon(true);
        updater.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // this event triggers when we release a key and then
        // we will move the space ship if the game is not over yet



        final int kc = e.getKeyCode();

        if (kc == KeyEvent.VK_ESCAPE) {
             System.exit(0);
}
        char key = e.getKeyChar();
        //if(!inputname && kc != KeyEvent.VK_SPACE) {
        //playerName = playerName + e.getKeyChar();
        //return;
        //}

        if (gameOver && kc == KeyEvent.VK_SPACE) {
            restartGame();
        }

        if (!gameStarted && kc == KeyEvent.VK_SPACE) {
            showMenu = false;
            jumpHeight = -7;
            sound.playSound("/jump.wav");
        }

        if(!inputname && kc == KeyEvent.VK_ENTER) {
            gameStarted = true;
        }

        if (kc == KeyEvent.VK_SPACE && gameStarted) {
            jumpHeight = -7;
            sound.playSound("/jump.wav");
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {



        final int b = e.getButton();

        if (b == MouseEvent.BUTTON1 && gameOver) {
            return;
        }

        if (b == MouseEvent.BUTTON1 && gameStarted) {
            jumpHeight = -7;
            sound.playSound("/jump.wav");
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // do nothing
    }

    @Override
    public void keyReleased(KeyEvent e) {
        final int kc = e.getKeyCode();
        char key = e.getKeyChar();
        Boolean b1 = Character.isLetter(key);

        if(!gameStarted && b1) {
            playerName = playerName + e.getKeyChar();
            return;
        }
        if(!gameStarted && kc == KeyEvent.VK_BACK_SPACE){
            if(playerName != null){
                playerName = playerName.substring(0, playerName.length() - 1);
            }
        }
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
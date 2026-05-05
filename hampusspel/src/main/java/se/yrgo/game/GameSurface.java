package se.yrgo.game;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
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
    enum difficulty{
        EASY,
        MEDIUM,
        HARD
    }

    private Highscore archive = new Highscore();
    private SoundPlayer sound = new SoundPlayer();
    private Map<String, Integer> highscoreEasy = new HashMap<>();
    private Map<String, Integer> highscoreMedium = new HashMap<>();
    private Map<String, Integer> highscoreHard = new HashMap<>();
    private static final long serialVersionUID = 6260582674762246325L;
    private static Logger logger = Logger.getLogger(GameSurface.class.getName());

    private static final double OBSTACLE_PIXELS_PER_MS = 0.25;

    // make some transient to get past boring serialization demands...
    private transient FrameUpdater updater;
    private boolean gameOver;
    private boolean gameStarted;
    private boolean once = true;
    private boolean showMenu = true;
    private boolean pause = true;
    private boolean difficultySelector = false;
    private transient List<Obstacle> obstacles;
    private transient List<Counter> counters;
    private Rectangle player;
    private transient BufferedImage playerImageSprite;
    private transient BufferedImage obstacleImageSprite;
    private int playerImageSpriteCount;
    private BufferedImage background;
    private BufferedImage nameBackground;
    private BufferedImage gameOverBackground;
    private boolean inputname = false;
    private String playerName = "";
    private File highScoreFileEasy = new File("src\\main\\resources\\highscore_easy.txt");
    private File highScoreFileMedium = new File("src\\main\\resources\\highscore_medium.txt");
    private File highScoreFileHard = new File("src\\main\\resources\\highscore_hard.txt");
    private difficulty myDifficulty = difficulty.MEDIUM;

    private int score;

    private double jumpHeight = 0;
    private double gravity;

    private int playerWidth = 85;
    private int playerHeight = 85;

    private long lastObstacleSpawnTime = 0;
    private static int OBSTACLE_SPAWN_INTERVAL;
    private int distance;

    public GameSurface(final int width) {
        try {
            highScoreFileEasy.createNewFile();
            highScoreFileMedium.createNewFile();
            highScoreFileHard.createNewFile();

        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to read or access high score");
        }
        highscoreEasy = archive.loadScore("highscore_easy.txt");
        highscoreMedium = archive.loadScore("highscore_medium.txt");
        highscoreHard = archive.loadScore("highscore_hard.txt");
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
        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/tree2.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /tree2.png");
            } else {
                this.obstacleImageSprite = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /tree2.png", ex);
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

        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/witch_name2.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /witch_name2.png");
            } else {
                this.nameBackground = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /witch_name.png2", ex);
        }

        try (InputStream spriteStream = GameSurface.class.getResourceAsStream("/gameover2.png")) {
            if (spriteStream == null) {
                logger.log(Level.WARNING, "Unable to load image resource: /gameover2.png");
            } else {
                this.gameOverBackground = ImageIO.read(spriteStream);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to load image resource: /gameover2.png", ex);
        }

        this.gameOver = false;
        this.gameStarted = false;
        this.once = true;
        this.showMenu = true;
        this.pause = true;
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
        try {
            drawSurface(g2d);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call this method when the graphics needs to be repainted on the graphics
     * surface.
     *
     * @param g the graphics to paint on
     */
    private void drawSurface(Graphics2D g) throws InterruptedException {
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
            int i = 0;
            for(String k : highscoreMedium.keySet()){
                if(i<10){
                    g.drawString((i + 1) + " - " + k + " : " + highscoreMedium.get(k), 25, y += newLine);
                    i += 1;
                }
            }
            return;
        }

        if (!showMenu && !gameStarted && !difficultySelector && !gameOver) {
            g.drawImage(nameBackground, 0, 0, null);
            g.drawImage(nameBackground, 1472, 0, null);
            g.setFont(new Font("Old English Text MT", Font.BOLD, 24));
            g.drawString("Please enter your name:", 710, 300);
            g.setFont(new Font("Ink Free", Font.BOLD, 20));
            g.drawString(playerName, 710, 350);
            super.repaint();
            return;
        }
        if(!showMenu && difficultySelector && !gameStarted){
            g.drawImage(gameOverBackground, 0, 0, null);
            g.drawImage(gameOverBackground, 1472, 0, null);
            g.setColor(Color.white);
            g.setFont(new Font("Old English Text MT", Font.BOLD, 40));
            g.drawString("Please select difficulty:", 580, 300);
            g.setFont(new Font("Old English Text MT", Font.PLAIN, 30));
            g.drawString("1: Easy", 710, 350);
            g.drawString("2: Medium", 710, 400);
            g.drawString("3: Hard", 710, 450);
            obstacles.clear();
            return;
        }

        if (gameOver) {
            Thread.sleep(50);
            if (once) {
                if(myDifficulty == difficulty.EASY){
                    if(!highscoreEasy.containsKey(playerName)){
                        highscoreEasy.put(playerName, score/20);
                        highscoreEasy = archive.sortScores(highscoreEasy);
                        archive.saveScore(highscoreEasy, "highscore_easy.txt");
                        once = false;
                    } else if (highscoreEasy.containsKey(playerName) && highscoreEasy.get(playerName) < score/20) {
                        highscoreEasy.remove(playerName);
                        highscoreEasy.put(playerName, score/20);
                        highscoreEasy = archive.sortScores(highscoreEasy);
                        archive.saveScore(highscoreEasy, "highscore_easy.txt");
                        once = false;
                    }

                }
                if(myDifficulty == difficulty.MEDIUM){
                    if(!highscoreMedium.containsKey(playerName)){
                        highscoreMedium.put(playerName, score/20);
                        highscoreMedium = archive.sortScores(highscoreMedium);
                        archive.saveScore(highscoreMedium, "highscore_medium.txt");
                        once = false;
                    } else if (highscoreMedium.containsKey(playerName) && highscoreMedium.get(playerName) < score/20) {
                        highscoreMedium.remove(playerName);
                        highscoreMedium.put(playerName, score/20);
                        highscoreMedium = archive.sortScores(highscoreMedium);
                        archive.saveScore(highscoreMedium, "highscore_medium.txt");
                        once = false;
                    }

                }
                if(myDifficulty == difficulty.HARD){
                    if(!highscoreHard.containsKey(playerName)){
                        highscoreHard.put(playerName, score/20);
                        highscoreHard = archive.sortScores(highscoreHard);
                        archive.saveScore(highscoreHard, "highscore_hard.txt");
                        once = false;
                    } else if (highscoreHard.containsKey(playerName) && highscoreHard.get(playerName) < score/20) {
                        highscoreHard.remove(playerName);
                        highscoreHard.put(playerName, score/20);
                        highscoreHard = archive.sortScores(highscoreHard);
                        archive.saveScore(highscoreHard, "highscore_hard.txt");
                        once = false;
                    }

                }

                else {
                    once = false;
                }

            }
            g.drawImage(gameOverBackground, 0, 0, null);
            g.drawImage(gameOverBackground, 1472, 0, null);
            g.setColor(Color.white);
            g.setFont(new Font("Old English Text MT", Font.PLAIN, 100));
            g.drawString("Game over!", 475, 150);
            if(myDifficulty == difficulty.EASY){
                g.setFont(new Font("Old English Text MT", Font.PLAIN, 40));
                int y = 220;
                int newLine = g.getFont().getSize() + 10;
                g.drawString("High Score - easy difficulty:", 420, y);
                g.setFont(new Font("Old English Text MT", Font.PLAIN, 30));
                int i = 0;
                for(String k : highscoreEasy.keySet()){
                    if(i<10){
                        g.drawString((i + 1) + " - " + k + " : " + highscoreEasy.get(k), 580, y += newLine);
                        i += 1;
                    }
                }

            } else if (myDifficulty == difficulty.MEDIUM) {
                g.setFont(new Font("Old English Text MT", Font.PLAIN, 40));
                int y = 220;
                int newLine = g.getFont().getSize() + 10;
                g.drawString("High Score - medium difficulty:", 420, y);
                g.setFont(new Font("Old English Text MT", Font.PLAIN, 30));
                int i = 0;
                for(String k : highscoreMedium.keySet()){
                    if(i<10){
                        g.drawString((i + 1) + " - " + k + " : " + highscoreMedium.get(k), 580, y += newLine);
                        i += 1;
                    }
                }

            }
            else if (myDifficulty == difficulty.HARD) {
                g.setFont(new Font("Old English Text MT", Font.PLAIN, 40));
                ;
                int y = 220;
                int newLine = g.getFont().getSize() + 10;
                g.drawString("High Score - hard difficulty:", 420, y);
                g.setFont(new Font("Old English Text MT", Font.PLAIN, 30));
                int i = 0;
                for(String k : highscoreHard.keySet()){
                    if(i<10){
                        g.drawString((i + 1) + " - " + k + " : " + highscoreHard.get(k), 580, y += newLine);
                        i += 1;
                    }
                }

            }
            g.setFont(new Font("Book Antiqua", Font.PLAIN, 30));
            g.drawString("Press SPACE to try again, ENTER to return to the main menu or D to change difficulty.", 150, 750);

            return;
        }
        if(myDifficulty == difficulty.EASY){
            gravity = 0.2;
            OBSTACLE_SPAWN_INTERVAL = 3000;
            distance = 900;
        } else if (myDifficulty == difficulty.MEDIUM) {
            gravity = 0.3;
            OBSTACLE_SPAWN_INTERVAL = 2500;
            distance = 850;
        }
        else if(myDifficulty == difficulty.HARD){
            gravity = 0.4;
            OBSTACLE_SPAWN_INTERVAL = 2000;
            distance = 800;
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

            double clampedVelocity = Math.max(-5, Math.min(5, jumpHeight));
            double angle = Math.toRadians(clampedVelocity * 3); // tweak multiplier for feel

            AffineTransform old = g.getTransform();

            try {
                g.rotate(angle,
                        player.x + playerWidth / 2.0,
                        player.y + playerHeight / 2.0);

                g.drawImage(
                        playerImageSprite,
                        player.x,
                        player.y,
                        player.x + playerWidth,
                        player.y + playerHeight,
                        offset,
                        0,
                        offset + playerWidth,
                        playerHeight,
                        null);
            } finally {
                g.setTransform(old);
            }
        }

        if (pause) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Press SPACE to jump!", 600, 300);
        }

        // draw the score
        if (!pause) {
            drawScore(g, d, false);
        }
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
        final Font scoreFont = new Font("Old English Text MT", Font.BOLD, 100);

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

    public void update(int time) throws InterruptedException {
        if (gameOver) {
            updater.interrupt();
            return;
        }

        if (!gameStarted) {
            return;
        }
        if (!pause) {
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
        int y2 = y1 + distance;
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

    private void unPause() throws InterruptedException {
        Dimension d = getSize();

        gameOver = false;
        once = true;
        pause = false;

        updater.wait();
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
        // if(!inputname && kc != KeyEvent.VK_SPACE) {
        // playerName = playerName + e.getKeyChar();
        // return;
        // }

        if (gameOver && kc == KeyEvent.VK_SPACE) {
            restartGame();
        }
        if(gameOver && kc == KeyEvent.VK_ENTER) {
            this.gameOver = false;
            this.gameStarted = false;
            this.once = true;
            this.showMenu = true;
            this.pause = true;
            restartGame();

        }
        if(gameOver && kc == KeyEvent.VK_D){
            this.gameOver = false;
            this.gameStarted = false;
            this.once = true;
            this.showMenu = false;
            this.difficultySelector = true;
            this.pause = true;
            playerName = playerName.substring(0, playerName.length() - 1);
            restartGame();
        }

        if (!gameStarted && showMenu && kc == KeyEvent.VK_SPACE) {
            showMenu = false;
            playerName = "";
        }

        if (!showMenu && kc == KeyEvent.VK_ENTER) {
            difficultySelector = true;
        }

        if(!showMenu && difficultySelector && kc == KeyEvent.VK_1){
            myDifficulty = difficulty.EASY;
            difficultySelector = false;
            gameStarted = true;
            pause = true;

        }

        if(!showMenu && difficultySelector && kc == KeyEvent.VK_2){
            myDifficulty = difficulty.MEDIUM;
            difficultySelector = false;
            gameStarted = true;
            pause = true;

        }

        if(!showMenu && difficultySelector && kc == KeyEvent.VK_3){
            myDifficulty = difficulty.HARD;
            difficultySelector = false;
            gameStarted = true;
            pause = true;

        }

        if (kc == KeyEvent.VK_SPACE && gameStarted) {
            jumpHeight = -7;
            sound.playSound("/jump.wav");
            pause = false;
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
        Boolean b2 = Character.isDigit(key);

        if (!gameStarted && b1 && !gameOver) {
            playerName = playerName + e.getKeyChar();
            if (playerName.length() > 20) {
                playerName = playerName.substring(0, playerName.length() - 1);
            }
            return;
        }
        if (!gameStarted && b2 && !gameOver) {
            playerName = playerName + e.getKeyChar();
            if (playerName.length() > 20) {
                playerName = playerName.substring(0, playerName.length() - 1);
            }
            return;
        }
        if (!gameStarted && kc == KeyEvent.VK_BACK_SPACE) {
            if (playerName != null) {
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
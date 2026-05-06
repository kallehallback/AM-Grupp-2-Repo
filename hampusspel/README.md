# Jumpy Witch – Demo Game Project

This is a simple Java Swing/AWT game.  
It is a Flappy Bird–style jumping game where a witch avoids obstacles, collects points, and competes for highscores.

---

## Building

Build the project using Maven:

    ./mvnw package

---

## Running

Run the application with:

    ./mvnw exec:java

---

## Gameplay Overview

- You control a witch that jumps to avoid obstacles (trees)
- Press SPACE or left mouse click to jump
- Avoid collisions and stay within the screen
- Score increases as you pass obstacles
- Choose between Easy, Medium, and Hard difficulty levels
- Highscores are saved locally per difficulty

---

## Architecture Overview

The project demonstrates a basic real-time game loop using Swing.

### Main Classes

1. App.java – Entry Point
   - Creates the main JFrame
   - Adds the GameSurface
   - Registers input listeners
   - Starts the application

2. GameSurface.java – Core Game Logic and Rendering
   Responsibilities:
   - Rendering (paintComponent, drawSurface)
   - Game state management (player, obstacles, score, menus)
   - Input handling (keyboard and mouse)

   Game flow:
   - Main menu
   - Name input
   - Difficulty selection
   - Gameplay
   - Game over screen

   Key features:
   - Time-based movement
   - Gravity and jumping mechanics
   - Collision detection
   - Sprite rendering
   - Highscore tracking
   - Sound effects

3. FrameUpdater.java – Game Loop
   - Runs on a separate thread
   - Calls update() and repaint()
   - Targets 60 FPS
   - Uses nanosecond timing

4. Obstacle.java
   - Represents obstacles (trees)
   - Stores position and creation time

5. Counter.java
   - Invisible scoring trigger between obstacles
   - Ensures score increments correctly

6. Highscore.java
   - Loads and saves highscores from text files
   - Format: name///score
   - Sorts scores in descending order

7. Player.java
   - Simple data class for player name and score

8. SoundPlayer.java
   - Plays sound effects (jump, score, game over)

9. BackgroundPanel.java (optional / unused)
   - Can render a background image
   - Not currently used in main flow

10. SortByScore.java (legacy / unused)
   - Old comparator, replaced by stream sorting

---

## How the Game Loop Works

1. Initialization:
   - App creates GameSurface
   - GameSurface starts FrameUpdater

2. Loop cycle (~60 FPS):
   - update(time)
     - Applies gravity
     - Moves player
     - Spawns obstacles
     - Checks collisions
     - Updates score
   - repaint()
     - Calls paintComponent()
     - Draws everything

3. Input:
   - SPACE / Mouse: jump
   - ENTER: navigate menus
   - ESC: exit game

---

## Key Design Concepts

- Time-based movement (not frame-based)
- Separation of update logic and rendering
- Simple state machine (menu → game → game over)
- Basic collision detection using Rectangle

---

## Important Notes for Safe Modifications

- Always update game state in update(), NOT in paintComponent()
- Painting should only read state, never modify it
- Movement should be time-based (speed * time)
- Be careful when modifying lists during iteration
- The game loop runs on a separate thread, so watch for concurrency issues

---

## Resources

Assets are loaded from:

    src/main/resources/

Includes:
- Images (witch, trees, backgrounds)
- Sound files (.wav)
- Highscore files:
  - highscore_easy.txt
  - highscore_medium.txt
  - highscore_hard.txt

---

## Possible Improvements

- Better object-oriented design (encapsulation)
- Cleaner separation of logic and rendering
- Use delta-time instead of absolute time
- Improve collision handling
- Add animations or effects
- Refactor into MVC or a more structured architecture
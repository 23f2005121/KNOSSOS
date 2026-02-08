package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

import games.spooky.gdx.nativefilechooser.NativeFileChooser;
import java.util.HashMap;
import java.util.Map;

/**
 * Main game class that serves as the central hub for Maze Runner.
 * Extends LibGDX's Game class to manage screens and shared resources.
 * <p>
 * Responsibilities:
 * - Loading and managing all game assets (textures, animations, sounds, music)
 * - Switching between different screens (menu, gameplay, options, etc.)
 * - Managing story mode progression (current level, save slots)
 * - Providing shared resources to all screens (SpriteBatch, Skin, animations)
 * - Handling background music for different levels
 * - Storing player preferences
 * <p>
 * This class uses the Singleton pattern through LibGDX's Game class -
 * there's only one instance that all screens reference for assets and navigation.
 */
public class MazeRunnerGame extends Game {

    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;

    // Shared rendering + UI
    private SpriteBatch spriteBatch;
    private Skin skin;


    // Preferences
    private GamePreferences preferences;

    // Animation Storage
    private Map<String, Animation<TextureRegion>> playerAnimations = new HashMap<>();
    private Map<String, Animation<TextureRegion>> slimeAnimations = new HashMap<>();
    private Map<String, Animation<TextureRegion>> skeletonAnimations = new HashMap<>();
    private Map<String, Animation<TextureRegion>> batAnimations = new HashMap<>();
    private Map<String, Animation<TextureRegion>> ghostAnimations = new HashMap<>();
    private Map<String, Animation<TextureRegion>> mageAnimations = new HashMap<>();

    // Legacy/Other Animations (For Mobs/Objects/Traps)
    private Animation<TextureRegion> enemyAnimation;
    private TextureRegion[][] mobTiles;
    private TextureRegion[][] objectTiles;
    private TextureRegion[] trapTiles; // New: Traps array

    // assets
    private Texture keyTexture;
    private TextureRegion keyRegion;
    private Texture heartTexture;
    private TextureRegion heartRegion;
    private Texture fastTexture;
    private TextureRegion fastRegion;
    private Texture livesTexture;
    private TextureRegion[] healthRegions;
    private Texture entryTexture;
    private TextureRegion entryRegion;
    private Texture exitTexture;
    private TextureRegion exitRegion;

    // XP & Stamina Bar assets
    private Texture meterFrameTex;
    private Texture xPFillTex;
    private Texture staminaFillTex;
    private Texture staminaLowFillTex;

    // Fireball projectile texture
    private Texture fireballTexture;
    private TextureRegion fireballRegion;

    // Music system
    private Map<Integer, Music> levelMusic = new HashMap<>(); // Maps level number to music track
    private Music currentMusic; // Currently playing music
    private boolean musicMuted = false; // Music mute state

    // Story mode progression tracking
    private int currentStoryLevel = 1; // Which story level the player is on (1-5)

    // Save system
    private int currentSaveSlot = 1; // Which save slot is currently active (1-3)


    private boolean isGamePaused = false;


    /**
     * Creates the main game instance.
     * The fileChooser parameter is required by the LibGDX launcher but not used in this game.
     *
     * @param fileChooser native file chooser (unused but required by launcher)
     */
    public MazeRunnerGame(NativeFileChooser fileChooser) {
        super();
    }

    /**
     * Called once when the game is first launched.
     * Initializes all systems, loads assets, and shows the intro screen.
     */
    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json"));
        preferences = new GamePreferences();

        loadAssets();
        VoiceManager.init();
        loadLevelMusic();

        // Play menu music (level 0)
        playMusicForLevel(0);

        // Show story screen immediately on launch
        setScreen(new StoryScreen(this));
    }

    // asset loading

    /**
     * Loads all game assets: animations, textures, and UI elements.
     * Called once during game initialization.
     */
    private void loadAssets() {
        // 1. Load Player Animations
        loadPlayerSheet("idle", 0.15f);
        loadPlayerSheet("move", 0.1f);
        loadPlayerSheet("attack", 0.05f);
        loadPlayerSheet("hurt", 0.1f);
        loadPlayerSheet("death", 0.2f);

        // 2. Load Slime (BigEnemy) Animations
        loadSlimeSheet("slime_idle", 0.15f);
        loadSlimeSheet("slime_move", 0.15f);
        loadSlimeSheet("slime_attack", 0.1f);
        loadSlimeSheet("slime_hurt", 0.1f);
        loadSlimeSheet("slime_death", 0.15f);

        // 3. Load Skeleton (NormalEnemy) Animations
        loadSkeletonSheet("skeleton_idle", 0.15f);
        loadSkeletonSheet("skeleton_move", 0.15f);
        loadSkeletonSheet("skeleton_attack", 0.1f);
        loadSkeletonSheet("skeleton_hurt", 0.1f);
        loadSkeletonSheet("skeleton_death", 0.15f);

        // 4. Load Bat (ChickenJockey) Animations
        loadBatSheet("bat_idle", 0.15f);
        loadBatSheet("bat_move", 0.1f);
        loadBatSheet("bat_attack", 0.1f);
        loadBatSheet("bat_hurt", 0.1f);
        loadBatSheet("bat_death", 0.15f);

        // 5. Load Ghost (ImmortalSnail) Animations
        loadGhostSheet("ghost_idle", 0.15f);
        loadGhostSheet("ghost_move", 0.15f);
        loadGhostSheet("ghost_attack", 0.1f);
        loadGhostSheet("ghost_hurt", 0.1f);
        loadGhostSheet("ghost_death", 0.15f);

        // 6. Load Mage Animations
        loadMageSheet("mage_idle", 0.15f);
        loadMageSheet("mage_move", 0.15f);
        loadMageSheet("mage_attack", 0.1f); // Charging up
        loadMageSheet("mage_hurt", 0.1f);
        loadMageSheet("mage_death", 0.15f);

        // load traps
        try {
            Texture trapsSheet = new Texture(Gdx.files.internal("traps.png"));
            TextureRegion[][] tmp = TextureRegion.split(trapsSheet, 16, 16);
            if (tmp.length > 0) {
                trapTiles = tmp[0]; // Gets the first row (the only row)
            }
        } catch (Exception e) {
            System.err.println("Could not load traps.png: " + e.getMessage());
        }

        // load legacy mobs
        try {
            Texture mobsSheet = new Texture(Gdx.files.internal("mobs.png"));
            mobTiles = TextureRegion.split(mobsSheet, 16, 16);

            // Create a default enemy animation for now
            Array<TextureRegion> enemyFrames = new Array<>();
            if (mobTiles.length > 0 && mobTiles[0].length > 1) {
                enemyFrames.add(mobTiles[0][0]);
                enemyFrames.add(mobTiles[0][1]);
            }
            enemyAnimation = new Animation<>(0.2f, enemyFrames);
        } catch (Exception e) {
            System.err.println("Could not load mobs.png: " + e.getMessage());
        }

        // entry and exit loading
        try {
            if (Gdx.files.internal("entry.png").exists()) {
                entryTexture = new Texture(Gdx.files.internal("entry.png"));
                entryRegion = new TextureRegion(entryTexture);
            }
            if (Gdx.files.internal("exit.png").exists()) {
                exitTexture = new Texture(Gdx.files.internal("exit.png"));
                exitRegion = new TextureRegion(exitTexture);
            }
        } catch (Exception e) {
            System.err.println("Could not load entry/exit textures: " + e.getMessage());
        }

        // loading objects
        try {
            Texture objectsSheet = new Texture(Gdx.files.internal("objects.png"));
            objectTiles = TextureRegion.split(objectsSheet, 16, 16);
        } catch (Exception e) {
            System.err.println("Could not load objects.png: " + e.getMessage());
        }

        // loading health ui here
        try {
            if (Gdx.files.internal("lives.png").exists()) {
                livesTexture = new Texture(Gdx.files.internal("lives.png"));
                healthRegions = TextureRegion.split(livesTexture, 16, 16)[0]; // Get the first (and only) row
            }
        } catch (Exception e) {
            System.err.println("Could not load lives.png: " + e.getMessage());
        }

        // XP and Stamina bar textures being loaded
        meterFrameTex = new Texture(Gdx.files.internal("meter.png"));
        xPFillTex = new Texture(Gdx.files.internal("xp_color.png"));
        staminaFillTex = new Texture(Gdx.files.internal("stamina_color.png"));
        staminaLowFillTex = new Texture(Gdx.files.internal("stamina_low_color.png"));

        // Load individual item textures (with fallback)
        try {
            if (Gdx.files.internal("key.png").exists()) {
                keyTexture = new Texture(Gdx.files.internal("key.png"));
                keyRegion = new TextureRegion(keyTexture);
            }
            if (Gdx.files.internal("heart.png").exists()) {
                heartTexture = new Texture(Gdx.files.internal("heart.png"));
                heartRegion = new TextureRegion(heartTexture);
            }
            if (Gdx.files.internal("fast.png").exists()) {
                fastTexture = new Texture(Gdx.files.internal("fast.png"));
                fastRegion = new TextureRegion(fastTexture);
            }

            // NEW: Load Fireball
            if (Gdx.files.internal("fireball.png").exists()) {
                fireballTexture = new Texture(Gdx.files.internal("fireball.png"));
                fireballRegion = new TextureRegion(fireballTexture);
            }
        } catch (Exception e) {
            System.err.println("Could not load individual item textures: " + e.getMessage());
        }

    }

    /**
     * Helper methods for loading different entity animation sheets.
     * These wrap the generic loader with type-specific targets.
     */
    private void loadPlayerSheet(String name, float frameDuration) {
        loadGenericSheet(name, frameDuration, playerAnimations);
    }

    private void loadSlimeSheet(String name, float frameDuration) {
        loadGenericSheet(name, frameDuration, slimeAnimations);
    }

    private void loadSkeletonSheet(String name, float frameDuration) {
        loadGenericSheet(name, frameDuration, skeletonAnimations);
    }

    private void loadBatSheet(String name, float frameDuration) {
        loadGenericSheet(name, frameDuration, batAnimations);
    }

    private void loadGhostSheet(String name, float frameDuration) {
        loadGenericSheet(name, frameDuration, ghostAnimations);
    }

    private void loadMageSheet(String name, float frameDuration) {
        loadGenericSheet(name, frameDuration, mageAnimations);
    }

    /**
     * Generic animation sheet loader for all entity types.
     * Expects a 3-row sprite sheet where:
     * - Row 0: East-facing animation frames
     * - Row 1: South-facing animation frames
     * - Row 2: North-facing animation frames
     * West-facing is generated by flipping the east-facing frames horizontally.
     *
     * @param name          base animation name (e.g., "idle", "move")
     * @param frameDuration how long each frame should display in seconds
     * @param targetMap     the map to store the animations in
     */
    private void loadGenericSheet(String name, float frameDuration, Map<String, Animation<TextureRegion>> targetMap) {
        String path = name + ".png";
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("Assets", "Could not find " + path);
            return;
        }
        try {
            Texture sheet = new Texture(Gdx.files.internal(path));
            TextureRegion[][] tmp = TextureRegion.split(sheet, 16, 16);

            Animation<TextureRegion> eastAnim = new Animation<>(frameDuration, tmp[0]);
            targetMap.put(name + "_EAST", eastAnim);

            Array<TextureRegion> westFrames = new Array<>();
            for (TextureRegion tex : tmp[0]) {
                TextureRegion flipped = new TextureRegion(tex);
                flipped.flip(true, false);
                westFrames.add(flipped);
            }
            Animation<TextureRegion> westAnim = new Animation<>(frameDuration, westFrames);
            targetMap.put(name + "_WEST", westAnim);

            Animation<TextureRegion> southAnim = new Animation<>(frameDuration, tmp[1]);
            targetMap.put(name + "_SOUTH", southAnim);

            Animation<TextureRegion> northAnim = new Animation<>(frameDuration, tmp[2]);
            targetMap.put(name + "_NORTH", northAnim);

        } catch (Exception e) {
            Gdx.app.error("Assets", "Error loading " + path, e);
        }
    }

    // =======================
    // ANIMATION GETTERS
    // =======================

    /**
     * Retrieves a player animation by state and direction.
     *
     * @param state     animation state (idle, move, attack, hurt, death)
     * @param direction facing direction (NORTH, SOUTH, EAST, WEST)
     * @return the requested animation, or a fallback if not found
     */
    public Animation<TextureRegion> getPlayerAnimation(String state, String direction) {
        return getAnimationFromMap(playerAnimations, state, direction, "idle_SOUTH");
    }

    public Animation<TextureRegion> getSlimeAnimation(String stateName, String direction) {
        return getAnimationFromMap(slimeAnimations, stateName, direction, "slime_idle_SOUTH");
    }

    public Animation<TextureRegion> getSkeletonAnimation(String stateName, String direction) {
        return getAnimationFromMap(skeletonAnimations, stateName, direction, "skeleton_idle_SOUTH");
    }

    public Animation<TextureRegion> getBatAnimation(String stateName, String direction) {
        return getAnimationFromMap(batAnimations, stateName, direction, "bat_idle_SOUTH");
    }

    public Animation<TextureRegion> getGhostAnimation(String stateName, String direction) {
        return getAnimationFromMap(ghostAnimations, stateName, direction, "ghost_idle_SOUTH");
    }

    public Animation<TextureRegion> getMageAnimation(String stateName, String direction) {
        return getAnimationFromMap(mageAnimations, stateName, direction, "mage_idle_SOUTH");
    }

    /**
     * Helper method to retrieve animations from a map with fallback support.
     *
     * @param map      the animation map to search in
     * @param state    animation state name
     * @param dir      direction suffix
     * @param fallback fallback key if the requested animation isn't found
     * @return the animation, or null if not found
     */
    private Animation<TextureRegion> getAnimationFromMap(Map<String, Animation<TextureRegion>> map, String state,
            String dir, String fallback) {
        String key = state + "_" + dir;
        if (map != null) {
            if (map.containsKey(key))
                return map.get(key);
            if (map.containsKey(fallback))
                return map.get(fallback);
        }
        return null;
    }

    // =======================
    // Screen navigation
    // =======================



    /**
     * Returns to the main menu and disposes of the game screen.
     */
    public void goToMenu() {
        setScreen(new MenuScreen(this));
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        // Switch back to menu music
        playMusicForLevel(0);
    }

    /**
     * Starts a new game (either story mode or endless mode).
     *
     * @param isStoryMode true for story mode, false for endless mode
     */
    public void goToGame(boolean isStoryMode) {
        setScreen(new GameScreen(this, isStoryMode, isStoryMode));
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }

        // Switch to gameplay music
        // Use level 1 music (levels.mp3) for endless or current story level
        if (isStoryMode) {
            playMusicForLevel(currentStoryLevel);
        } else {
            playMusicForLevel(1);
        }
    }

    /**
     * Opens the options menu with a parent screen to return to.
     *
     * @param parent the screen to return to when closing options
     */
    public void goToOptions(Screen parent) {
        setScreen(new OptionsMenu(this, parent));
    }

    /**
     * Opens the options menu (no parent screen specified).
     */
    public void goToOptions() {
        goToOptions(null);
    }

    /**
     * Shows the victory screen (story mode completed).
     */
    public void goToVictory() {
        setScreen(new VictoryScreen(this));
    }

    /**
     * Shows the game over screen.
     *
     * @param wasStoryMode whether the player was in story mode or endless mode
     */
    public void goToGameOver(boolean wasStoryMode) {
        setScreen(new GameOverScreen(this, wasStoryMode));
    }

    // =======================
    // Story Logic
    // =======================

    /**
     * Advances to the next story level and starts it.
     */
    public void goToNextStoryLevel() {
        advanceStoryLevel();
        setScreen(new GameScreen(this, true, false));
        // Ensure music continues (or changes if we had specific level tracks)
        playMusicForLevel(currentStoryLevel);
    }

    /**
     * Gets the current story level number (1-5).
     *
     * @return current story level
     */
    public int getCurrentStoryLevel() {
        return currentStoryLevel;
    }

    /**
     * Increments the story level counter.
     */
    public void advanceStoryLevel() {
        currentStoryLevel++;
    }

    /**
     * Restarts story mode from level 1.
     */
    public void restartStory() {
        currentStoryLevel = 1;
        setScreen(new GameScreen(this, true, false));
        // Reset to level 1 music
        playMusicForLevel(currentStoryLevel);
    }

    /**
     * Sets which save slot is currently active.
     *
     * @param slot save slot number (1-3)
     */
    public void setCurrentSaveSlot(int slot) {
        this.currentSaveSlot = slot;
    }

    /**
     * Gets the currently active save slot number.
     *
     * @return current save slot (1-3)
     */
    public int getCurrentSaveSlot() {
        return currentSaveSlot;
    }

    // audio

    /**
     * Gets the shared sprite batch used by all screens.
     *
     * @return the main sprite batch
     */
    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    /**
     * Gets the UI skin (theme and styles).
     *
     * @return the UI skin
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * Gets the player preferences (key bindings, volumes, etc.).
     *
     * @return the preferences object
     */
    public GamePreferences getPreferences() {
        return preferences;
    }

    // texure/animation getters

    /**
     * Gets the default enemy animation (legacy fallback).
     *
     * @return default enemy animation
     */
    public Animation<TextureRegion> getEnemyAnimation() {
        return enemyAnimation;
    }

    /**
     * Gets a specific texture from the mobs sprite sheet.
     *
     * @param row row index in the sprite sheet
     * @param col column index in the sprite sheet
     * @return the texture region, or a fallback if not found
     */
    public TextureRegion getEnemyTexture(int row, int col) {
        if (mobTiles != null && row < mobTiles.length && col < mobTiles[0].length) {
            return mobTiles[row][col];
        }
        if (enemyAnimation != null && enemyAnimation.getKeyFrames().length > 0) {
            return enemyAnimation.getKeyFrame(0);
        }
        return null;
    }

    /**
     * Gets a specific texture from the objects sprite sheet.
     *
     * @param row row index in the sprite sheet
     * @param col column index in the sprite sheet
     * @return the texture region, or null if not found
     */
    public TextureRegion getObjectTexture(int row, int col) {
        if (objectTiles != null && row < objectTiles.length && col < objectTiles[0].length) {
            return objectTiles[row][col];
        }
        return null;
    }

    /**
     * Retrieves the correct trap texture from the 48x16 sheet.
     * Col 0: Slowness Trap (Type 1)
     * Col 1: Spike Trap (Type 0)
     * Col 2: Vine Trap (Type 2)
     */
    public TextureRegion getTrapTexture(int type) {
        if (trapTiles != null && trapTiles.length >= 3) {
            if (type == 1)
                return trapTiles[0]; // Slowness (1st Column)
            if (type == 0)
                return trapTiles[1]; // Spikes (2nd Column)
            if (type == 2)
                return trapTiles[2]; // Vines (3rd Column)
        }
        return null;
    }

    /**
     * Gets the key texture.
     *
     * @return key texture region
     */
    public TextureRegion getKeyTexture() {
        if (keyRegion != null)
            return keyRegion;
        return getObjectTexture(0, 3); // Fallback to old sprite sheet position
    }

    /**
     * Gets the heart (life) texture.
     *
     * @return heart texture region
     */
    public TextureRegion getHeartTexture() {
        if (heartRegion != null)
            return heartRegion;
        return getObjectTexture(0, 0); // Fallback to old sprite sheet position
    }

    /**
     * Gets the speed boost texture.
     *
     * @return speed boost texture region
     */
    public TextureRegion getFastTexture() {
        if (fastRegion != null)
            return fastRegion;
        return getObjectTexture(1, 2); // Fallback to old sprite sheet position
    }

    /**
     * Gets the fireball projectile texture.
     *
     * @return fireball texture region
     */
    public TextureRegion getFireballTexture() {
        if (fireballRegion != null)
            return fireballRegion;
        // Fallback to damage powerup icon if file missing
        return getObjectTexture(3, 5);
    }

    /**
     * Gets the meter frame texture.
     *
     * @return frame texture
     */
    public Texture getMeterFrame() {
        return meterFrameTex; } // get the frame texture

    /**
     * Gets the xp fill color texture.
     *
     * @return the xp fill color texture
     */
    public Texture getMeterFill() {
        return xPFillTex; } // get the xp color texture

    /**
     * Gets the stamina fill color texture.
     *
     * @return stamina fill color texture
     */
    public Texture getStaminaFill() {
        return staminaFillTex; } //get the stamina color texture

    /**
     * Gets the low fill color texture.
     *
     * @return low fill texture
     */
    public Texture getStaminaLowFill() {
        return staminaLowFillTex; // get the low stamina color
    }

    /**
     * Gets the heart textures
     *
     * @return a textureregion of the lives.png
     */
    public TextureRegion[] getHealthRegions() {
        return healthRegions;
    }

    public TextureRegion getEntryTexture() {
        return entryRegion != null ? entryRegion : getObjectTexture(1, 1); // fallback
    }

    public TextureRegion getExitTexture() {
        return exitRegion != null ? exitRegion : getObjectTexture(1, 1); // fallback
    }

    // music

    /**
     * Loads background music for the menu and all 5 story levels.
     * Falls back to menu music if a level's music file is missing.
     */
    private void loadLevelMusic() {
        // Load menu music (level 0)
        try {
            Music menuMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
            menuMusic.setLooping(true);
            levelMusic.put(0, menuMusic);
        } catch (Exception e) {
            Gdx.app.error("MazeRunnerGame", "Could not load background.mp3");
        }

        // 2. Load Gameplay Music (levels.mp3)
        Music gameplayMusic = null;
        try {
            if (Gdx.files.internal("levels.mp3").exists()) {
                gameplayMusic = Gdx.audio.newMusic(Gdx.files.internal("levels.mp3"));
                gameplayMusic.setLooping(true);
            }
        } catch (Exception e) {
            Gdx.app.error("MazeRunnerGame", "Could not load levels.mp3");
        }

        // Fallback: If levels.mp3 is missing, use background.mp3
        if (gameplayMusic == null) {
            gameplayMusic = levelMusic.get(0);
        }

        // Map gameplay music to levels 1 through 100 (covers Story and Endless)
        for (int i = 1; i <= 100; i++) {
            levelMusic.put(i, gameplayMusic);
        }
    }

    /**
     * Plays the music track for a specific level.
     * Stops the current music and starts the new track.
     *
     * @param level the level number (0 for menu, 1-5 for story levels)
     */
    public void playMusicForLevel(int level) {
        if (musicMuted)
            return;

        // Get the correct music for this level (or fallback to menu)
        Music newMusic = levelMusic.getOrDefault(level, levelMusic.get(0));

        // OPTIMIZATION: If the correct track is already playing, don't restart it
        // This makes the transition between Story Levels seamless (music keeps flowing)
        if (currentMusic == newMusic && currentMusic.isPlaying()) {
            // Ensure volume is correct (in case we came from a paused state)
            isGamePaused = false;
            updateMusicVolume();
            return;
        }

        // Stop old music
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.stop();
        }

        // Start new music
        if (newMusic != null) {
            currentMusic = newMusic;
            isGamePaused = false; // Reset pause muffling
            updateMusicVolume(); // Apply volume settings
            currentMusic.play();
        }
    }

    /**
     * Sets the volume based on the slider preference AND the pause state.
     * If game is paused, volume is halved.
     */
    private void updateMusicVolume() {
        if (currentMusic != null) {
            float masterVolume = preferences.getMusicVolume();

            // If in-game pause is active, halve the volume
            if (isGamePaused) {
                currentMusic.setVolume(masterVolume * 0.3f);
            } else {
                currentMusic.setVolume(masterVolume);
            }
        }
    }


    /**
     * Plays a random level track for endless mode.
     * Randomly selects from levels 1-5.
     */
    public void playMusicForEndlessMode() {
        // Randomly select from level 1-5 music
        int randomLevel = 1 + (int) (Math.random() * 5);
        playMusicForLevel(randomLevel);
    }


    /**
     * Stops the currently playing music.
     */
    public void stopMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.stop();
        }
    }


    /**
     * Pauses the currently playing music.
     */
    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    /**
     * Resumes the paused music.
     */
    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    /**
     * Sets the music volume.
     *
     * @param volume volume level (0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        updateMusicVolume();
    }


    /**
     * Called by GameScreen when ESC is pressed.
     * Toggles the "muffled" (halved volume) effect.
     */
    public void setInGamePaused(boolean paused) {
        this.isGamePaused = paused;
        updateMusicVolume();
    }

    /**
     * Toggles music mute on/off.
     */
    public void toggleMusicMute() {
        musicMuted = !musicMuted;
        if (musicMuted) {
            pauseMusic();
        } else {
            resumeMusic();
        }
    }

    /**
     * Checks if music is currently muted.
     *
     * @return true if music is muted, false otherwise
     */
    public boolean isMusicMuted() {
        return musicMuted;
    }

    /**
     * Cleans up all resources when the game is closed.
     * Disposes of textures, music, and other assets to prevent memory leaks.
     */
    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().hide();
            getScreen().dispose();
        }

        if (meterFrameTex != null) {
            meterFrameTex.dispose();
        }

        if (xPFillTex != null) {
            xPFillTex.dispose();
        }

        if (staminaFillTex != null) {
            staminaFillTex.dispose();
        }

        if (staminaLowFillTex != null) {
            staminaLowFillTex.dispose();
        }

        spriteBatch.dispose();
        skin.dispose();

        // Dispose all music tracks
        for (Music music : levelMusic.values()) {
            if (music != null) {
                music.dispose();
            }
        }
    }
}

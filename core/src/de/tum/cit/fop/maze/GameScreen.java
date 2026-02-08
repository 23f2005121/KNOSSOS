package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;

/**
 * The main gameplay screen where all the action happens.
 * Handles rendering the maze, player movement, enemy AI, collectibles, combat,
 * scoring, achievements, pause menu, and save/load functionality.
 * <p>
 * Works in two modes:
 * - Story Mode: Play through 5 pre-designed levels with fixed layouts
 * - Endless Mode: Procedurally generated levels that get progressively harder
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private boolean isStoryMode;

    private MapManager mapManager;
    private Player player;
    private Item key;
    private Item exit;

    // Lists/Arrays for all of our collectibles, obstacles and bullets
    private List<CollectableLives> hearts = new ArrayList<>();
    private List<CollectablePowerUps> powerUps = new ArrayList<>();
    private Array<Obstacle> obstacles = new Array<>();
    private List<Weapons> weapons = new ArrayList<>();
    private Array<MageBolt> bolts = new Array<>();
    private final Array<GameObject> renderQueue = new Array<>();


    // Tileset Addressing/Textures
    private static final int DAMAGE_ROW = 3, DAMAGE_COL = 5;
    private static final int EXIT_ROW = 1, EXIT_COL = 1;
    private Item entry;

    // UI & Console
    private Hud hud;
    private Stage pauseStage;
    private boolean isPaused = false;
    private InputAdapter gameInputProcessor;
    private DevConsole devConsole;
    private Texture blackTexture;

    private Texture background;

    // Achievement System Variables
    private AchievementHud achievementHud = new AchievementHud();
    private float levelTimer = 0f;
    private int pauseCount = 0;
    private boolean usedHeartThisLevel = false;
    private boolean tookDamageThisLevel = false;
    private int lastPlayerLives = 3;

    // Viewport & Zoom
    private int zoomPercentage = 100;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Score related variables
    private int level = 0;
    private boolean victoryTriggered = false;
    private boolean isLevelCompleting = false;
    private float completionTimer = 0f;
    private boolean isGameOverTriggered = false;

    // Screen Shake variables
    private float shakeTimer = 0f;
    private float shakeIntensity = 0f;

    // Save related variables
    private float autoSaveTimer = 0f;
    private boolean isLoadingFromSave = false;

    /**
     * Creates a new gameplay screen and initializes all systems.
     * Can either start a fresh level or load from a save file.
     *
     * @param game        reference to the main game for assets and navigation
     * @param isStoryMode true for story mode, false for endless mode
     * @param shouldLoad  if true and in story mode, attempts to load from save file
     */
    public GameScreen(MazeRunnerGame game, boolean isStoryMode, boolean shouldLoad) {
        this.game = game;
        this.isStoryMode = isStoryMode;
        this.level = game.getCurrentStoryLevel();

        // Set up camera and viewport (15x15 tiles at 16 pixels each)
        camera = new OrthographicCamera();
        viewport = new FitViewport(12 * 14, 12 * 14, camera);

        mapManager = new MapManager();

        // Initialize HUD and pause menu
        hud = new Hud(game.getSpriteBatch(), game.getSkin().getFont("font"));
        setupPauseMenu();

        // 1x1 black pixel for dimming the screen
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.6f);
        pixmap.fill();
        blackTexture = new Texture(pixmap);

        background = new Texture(Gdx.files.internal("game_background.png"));
        pixmap.dispose();

        // Try to load save if in Story mode and explicitly requested
        boolean loadedSave = false;
        if (isStoryMode && shouldLoad) {
            int saveSlot = game.getCurrentSaveSlot();
            if (saveSlot > 0) {
                GameStateManager.SaveState save = GameStateManager.loadGame(saveSlot);
                if (save != null) {
                    loadFromSave(save);
                    loadedSave = true;
                    Gdx.app.log("SaveSystem", "Loaded game from slot " + saveSlot);
                }
            }
        }

        // If no save loaded (or not requested), start new level
        if (!loadedSave) {
            startNewLevel();
        }

        setupInputProcessor();

        // Initialize the developer console for cheat commands
        devConsole = new DevConsole(game.getSkin(), this, player, mapManager);
    }

    /**
     * Finds a random walkable floor tile that hasn't been used yet.
     * Prevents items and enemies from spawning on top of each other.
     *
     * @param occupied list of already-used positions to avoid
     * @return [x, y] coordinates of an empty floor tile
     */
    private float[] getUniqueFloorPosition(List<float[]> occupied) {
        float[] pos;
        boolean overlap;
        do {
            overlap = false;
            pos = mapManager.getRandomFloorPosition();
            for (float[] used : occupied) {
                if (MathUtils.isEqual(pos[0], used[0]) && MathUtils.isEqual(pos[1], used[1])) {
                    overlap = true;
                    break;
                }
            }
        } while (overlap); // Keep trying until we find an empty spot
        occupied.add(pos); // Mark this position as used
        return pos;
    }

    /**
     * Starts a new level by generating the map, spawning entities, and resetting
     * state.
     * Handles both story mode (fixed layouts) and endless mode (procedural
     * generation).
     */
    public void startNewLevel() {

        if (level <= 1) {
            ScoreManager.getInstance().resetTotalScore(); // You might need to add this method to ScoreManager
        }

        /*
         * if mapdata is not null, it means a game is currently running. Thus, what we
         * need
         * to do is move on to the next level
         */

        if (mapManager.getMapData() != null) {
            if (isStoryMode) {
                game.advanceStoryLevel();
                this.level = game.getCurrentStoryLevel();
            } else {
                this.level++;
            }
        }

        victoryTriggered = false;
        levelTimer = 0; // resetting timer to help with achievements
        usedHeartThisLevel = false;
        tookDamageThisLevel = false;
        bolts.clear(); // clear old projectiles on new level

        // reset the score every level
        ScoreManager.getInstance().startNewLevel();

        // Play level start sound
        VoiceManager.playLevel("level_start");

        mapManager.loadLevel(level, isStoryMode);
        List<float[]> occupiedSpots = new ArrayList<>();
        weapons.clear();

        // Spawn weapon upgrades on specific levels
        if (level == 2) {
            float[] pos = getUniqueFloorPosition(occupiedSpots);
            weapons.add(new Weapons(pos[0], pos[1], 16, game.getObjectTexture(0, 7), 2));
        } else if (level == 3) {
            float[] pos = getUniqueFloorPosition(occupiedSpots);
            weapons.add(new Weapons(pos[0], pos[1], 16, game.getObjectTexture(0, 8), 3));
        }

        float tileSize = mapManager.getTileSize();

        float playerSize = 12;
        float offset = (tileSize - playerSize) / 2f;

        float startX, startY;
        if (isStoryMode) {
            startX = mapManager.startX * tileSize + offset;
            startY = mapManager.startY * tileSize + offset;
            occupiedSpots.add(new float[] { mapManager.startX * tileSize, mapManager.startY * tileSize });
        } else {
            float[] rnd = getUniqueFloorPosition(occupiedSpots);
            startX = rnd[0] + offset;
            startY = rnd[1] + offset;
        }

        // spawn the entry point
        entry = new Item(startX - offset, startY - offset, 16, game.getEntryTexture());

        if (player == null) {
            player = new Player(startX, startY, playerSize, playerSize);
        } else {
            player.setPosition(startX, startY);
            player.clearInput();
        }
        lastPlayerLives = player.getLives();

        if (isStoryMode && mapManager.getLevelInfo() != null) {
            occupiedSpots.add(new float[] { mapManager.keyX * tileSize, mapManager.keyY * tileSize });
            occupiedSpots.add(new float[] { mapManager.exitX * tileSize, mapManager.exitY * tileSize });

            key = new Item(mapManager.keyX * tileSize + offset, mapManager.keyY * tileSize + offset, 12,
                    game.getKeyTexture());

            // exit is created
            exit = new Item(mapManager.exitX * tileSize, mapManager.exitY * tileSize, 16,
                    game.getExitTexture());
            exit.setActive(true);
        } else {
            float[] kPos = getUniqueFloorPosition(occupiedSpots);
            key = new Item(kPos[0] + offset, kPos[1] + offset, 12,
                    game.getKeyTexture());

            float[] ePos = getUniqueFloorPosition(occupiedSpots);
            exit = new Item(ePos[0], ePos[1], 16, game.getExitTexture());
            exit.setActive(true);
        }

        // Spawn collectibles
        hearts.clear();
        powerUps.clear();

        float[] hPos = getUniqueFloorPosition(occupiedSpots);
        hearts.add(new CollectableLives(hPos[0], hPos[1], 16, game.getHeartTexture(), 1));

        float[] pPos = getUniqueFloorPosition(occupiedSpots);
        powerUps.add(new CollectablePowerUps(pPos[0] + offset, pPos[1] + offset, 12, game.getFastTexture(),
                CollectablePowerUps.Type.SPEED, 5f));

        float[] dPos = getUniqueFloorPosition(occupiedSpots);
        powerUps.add(new CollectablePowerUps(dPos[0] + offset, dPos[1] + offset, 12,
                game.getObjectTexture(DAMAGE_ROW, DAMAGE_COL),
                CollectablePowerUps.Type.DAMAGE, 10f));

        // Spawn obstacles (enemies and traps)
        obstacles.clear();
        if (isStoryMode && mapManager.getLevelInfo() != null) {
            // Story mode has pre-defined enemy and trap positions
            for (int[] p : mapManager.getLevelInfo().traps) {
                occupiedSpots.add(new float[] { p[0] * tileSize, p[1] * tileSize });
            }
            for (int[] p : mapManager.getLevelInfo().enemies) {
                occupiedSpots.add(new float[] { p[0] * tileSize, p[1] * tileSize });
            }

            // Spawn traps
            for (int[] pos : mapManager.getLevelInfo().traps) {
                float tx = pos[0] * tileSize;
                float ty = pos[1] * tileSize;
                int type = MathUtils.random(0, 2); // Random trap type
                TextureRegion tex = game.getTrapTexture(type);
                obstacles.add(new Traps(tx, ty, 16, 16, tex, type));
            }

            // Spawn enemies with variety
            int currentMages = 0;
            int mageLimit = 5; // Cap on mages per level for balance
            for (int[] pos : mapManager.getLevelInfo().enemies) {
                float ex = pos[0] * tileSize + offset;
                float ey = pos[1] * tileSize + offset;
                int roll = MathUtils.random(0, 99);
                // 10% mage, 50% normal, 25% jockey, 15% big
                if (roll < 10 && currentMages < mageLimit) {
                    obstacles.add(new MageEnemy(ex, ey, game));
                    currentMages++;
                } else if (roll < 60) {
                    obstacles.add(new NormalEnemy(ex, ey, game));
                } else if (roll < 85) {
                    obstacles.add(new ChickenJockey(ex, ey, game));
                } else {
                    obstacles.add(new BigEnemy(ex, ey, game));
                }
            }

            // Spawn the immortal snail (special enemy)
            float[] sPos = getUniqueFloorPosition(occupiedSpots);
            obstacles.add(new ImmortalSnail(sPos[0], sPos[1], game));
            Gdx.app.log("Game", "The Snail has entered the maze...");
        } else if (!isStoryMode) {
            // Endless mode uses procedural generation
            spawnEndlessEntities(occupiedSpots, tileSize, offset);
        }
    }

    /**
     * Spawns enemies, traps, and items for endless mode with progressive
     * difficulty.
     * Enemy count and health increase with each level.
     *
     * @param occupied list of already-used tile positions
     * @param tileSize size of each map tile in pixels
     * @param offset   centering offset for entities
     */
    private void spawnEndlessEntities(List<float[]> occupied, float tileSize, float offset) {
        // spawning weapons randomly
        if (level >= 6 && player.getWeaponDamage() < 3) {
            float[] pos = getUniqueFloorPosition(occupied);
            weapons.add(new Weapons(pos[0], pos[1], 16, game.getObjectTexture(0, 8), 3));
        } else if (level >= 4 && player.getWeaponDamage() < 2) {
            float[] pos = getUniqueFloorPosition(occupied);
            weapons.add(new Weapons(pos[0], pos[1], 16, game.getObjectTexture(0, 7), 2));
        }

        // enemy cap at 25, and the health is maximum 10
        int enemyCount = Math.min(5 + (level * 2), 25);
        int healthBonus = (level > 10) ? (level - 10) : 0;

        // randomly spawning enemies, using mathutils.random
        // to assign a number and then spawning enemy accordingly
        for (int i = 0; i < enemyCount; i++) {
            float[] pos = getUniqueFloorPosition(occupied);
            float ex = pos[0] + offset;
            float ey = pos[1] + offset;
            int roll = MathUtils.random(0, 99);
            Enemy newEnemy;

            if (level <= 2)
                newEnemy = new NormalEnemy(ex, ey, game);
            else if (level == 3)
                newEnemy = (roll < 80) ? new NormalEnemy(ex, ey, game) : new ChickenJockey(ex, ey, game);
            else if (level == 4)
                newEnemy = (roll < 70) ? new NormalEnemy(ex, ey, game) : new ChickenJockey(ex, ey, game);
            else if (level == 5) {
                if (roll < 70)
                    newEnemy = new NormalEnemy(ex, ey, game);
                else if (roll < 90)
                    newEnemy = new ChickenJockey(ex, ey, game);
                else
                    newEnemy = new BigEnemy(ex, ey, game);
            } else if (level == 6) {
                if (roll < 60)
                    newEnemy = new NormalEnemy(ex, ey, game);
                else if (roll < 80)
                    newEnemy = new ChickenJockey(ex, ey, game);
                else
                    newEnemy = new BigEnemy(ex, ey, game);
            } else { // introducing the mage enemy on endless
                if (roll < 10)
                    newEnemy = new MageEnemy(ex, ey, game);
                else if (roll < 60)
                    newEnemy = new NormalEnemy(ex, ey, game);
                else if (roll < 80)
                    newEnemy = new ChickenJockey(ex, ey, game);
                else
                    newEnemy = new BigEnemy(ex, ey, game);
            }

            if (healthBonus > 0)
                newEnemy.addExtraHealth(healthBonus);
            obstacles.add(newEnemy);
        }

        // adding traps randomly on endless
        int trapCount = 3 + (level / 2);
        for (int i = 0; i < trapCount; i++) {
            float[] p = getUniqueFloorPosition(occupied);
            int type = MathUtils.random(0, 2);
            TextureRegion tex = (type == 0) ? game.getObjectTexture(2, 4)
                    : (type == 1) ? game.getObjectTexture(1, 2) : game.getObjectTexture(0, 1);
            obstacles.add(new Traps(p[0], p[1], 16, 16, tex, type));
        }

        // adding the snail in endless mode
        float[] sPos = getUniqueFloorPosition(occupied);
        obstacles.add(new ImmortalSnail(sPos[0], sPos[1], game));
    }

    /**
     * Adjusts the camera zoom level and clamps it between 50% and 150%.
     *
     * @param amount how much to change zoom by (positive zooms in, negative zooms
     *               out)
     */
    private void zoomCamera(int amount) {
        zoomPercentage = MathUtils.clamp(zoomPercentage + amount, 50, 150);
        camera.zoom = 100f / zoomPercentage;
    }

    /**
     * Sets up keyboard input handling for gameplay.
     * Handles movement, pause, fullscreen, zoom, and developer console.
     */
    private void setupInputProcessor() {
        gameInputProcessor = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                // Escape toggles pause menu
                if (keycode == Input.Keys.ESCAPE) {
                    togglePause();
                    return true;
                }
                // F toggles fullscreen
                if (keycode == game.getPreferences().getKeyFullscreen()) {
                    if (Gdx.graphics.isFullscreen())
                        Gdx.graphics.setWindowedMode(1280, 720);
                    else
                        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    return true;
                }
                // I zooms in
                if (keycode == game.getPreferences().getKeyZoomIn()) {
                    zoomCamera(5);
                    return true;
                }
                // O zooms out
                if (keycode == game.getPreferences().getKeyZoomOut()) {
                    zoomCamera(-5);
                    return true;
                }
                // T opens developer console
                if (keycode == game.getPreferences().getKeyConsole()) {
                    player.clearInput();
                    devConsole.setVisible(true);
                    Gdx.input.setInputProcessor(devConsole.getStage());
                    return true;
                }
                // Forward all other keys to the player
                player.keyDown(keycode, game.getPreferences());
                return true;
            }

            @Override
            public boolean keyUp(int keycode) {
                player.keyUp(keycode, game.getPreferences());
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                // Mouse wheel zooms camera
                zoomCamera((int) amountY * -5);
                return true;
            }
        };
        Gdx.input.setInputProcessor(gameInputProcessor);
    }

    /**
     * Main render loop called every frame.
     * Updates all game logic (when not paused) and draws everything.
     *
     * @param delta time passed since last frame in seconds
     */
    @Override
    public void render(float delta) {

        if (!isPaused) {

            levelTimer += delta;

            // time decay for scoring
            ScoreManager.getInstance().update(delta);

            player.update(delta, mapManager, game.getPreferences(), game);
            // checking if the animation is finished
            if (player.isDeathAnimationFinished() && !isGameOverTriggered) {
                isGameOverTriggered = true;
                // save the "endless session" score
                try {
                    if (!isStoryMode) {
                        ScoreManager.getInstance().finalizeLoss();
                        LeaderboardManager.getInstance().addScore(
                                ScoreManager.getInstance().getTotalGameScore(),
                                level);
                    } else {
                        // story mode perma death
                        GameStateManager.deleteSave(game.getCurrentSaveSlot());
                        Gdx.app.log("GameScreen", "Player died. Save file deleted.");
                    }
                    game.goToGameOver(this.isStoryMode);
                } catch (Exception e) {
                    game.goToMenu();
                }
                return;
            }

            if (!isLevelCompleting) {
                // Enemy/Projectile logic
                java.util.Iterator<Obstacle> iter = obstacles.iterator();
                while (iter.hasNext()) {
                    Obstacle o = iter.next();
                    o.update(delta, player, mapManager);

                    if (o instanceof MageEnemy) {
                        MageEnemy mage = (MageEnemy) o;
                        if (mage.hasShotReady()) {
                            bolts.add(new MageBolt(mage.getX() + 4, mage.getY() + 4,
                                    player.getX() + 4, player.getY() + 4,
                                    game.getFireballTexture()));
                        }
                    }

                    if (o instanceof Enemy) {
                        Enemy enemy = (Enemy) o;
                        if (player.getLives() > 0) {
                            if (player.isAttacking()) {
                                com.badlogic.gdx.math.Rectangle hitbox = player.getAttackHitbox();
                                if (hitbox != null && hitbox.overlaps(enemy.getBounds())) {
                                    enemy.takeHit(player.getWeaponDamage(), player.getX(), player.getY());
                                }
                            }
                        }
                        if (enemy.isDead()) {
                            int xpReward = (enemy instanceof ChickenJockey) ? 5
                                    : (enemy instanceof NormalEnemy) ? 10 : 20;
                            if (MasteryManager.getInstance().addExperience(xpReward)) {
                                hud.showLevelUpNotification();
                            }
                            hud.addScorePopup("XP +" + xpReward, true);
                            iter.remove();
                        }
                    }
                }

                java.util.Iterator<MageBolt> bIter = bolts.iterator();
                while (bIter.hasNext()) {
                    MageBolt bolt = bIter.next();
                    bolt.update(delta, mapManager);
                    if (bolt.getBounds().overlaps(player.getBounds())) {
                        player.takeDamage(1);
                        bolt.setInactive();
                    }
                    if (!bolt.isActive())
                        bIter.remove();
                }

            } else {
                completionTimer += delta;
                if (completionTimer > 5.0f) {
                    isLevelCompleting = false;
                    completionTimer = 0;
                    if (isStoryMode && level >= 5)
                        game.goToVictory();
                    else
                        startNewLevel();
                }
            }

            if (player.getLives() < lastPlayerLives) {
                tookDamageThisLevel = true;
                shakeTimer = 0.3f;
                shakeIntensity = 4.0f;
                ScoreManager.getInstance().subtractPenalty("Damage Taken", 100);
                hud.addScorePopup("Damage -100", false);
                lastPlayerLives = player.getLives();
            }

            checkEntityCollisions(); // Check for item pickups and exit collision
            updateCamera(); // Make camera follow the player
        }

        // draw everything
        ScreenUtils.clear(0, 0, 0, 1);

        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getSpriteBatch().end();

        viewport.apply();
        game.getSpriteBatch().setProjectionMatrix(camera.combined);

        game.getSpriteBatch().begin();
        // Draw in layers: map, items, enemies, player
        mapManager.render(game.getSpriteBatch(), camera);

        renderQueue.clear();
        if (entry != null) renderQueue.add(entry);
        if (key != null && key.isActive()) renderQueue.add(key);
        if (exit != null && exit.isActive()) renderQueue.add(exit);
        for (CollectableLives h : hearts) if (h.isActive()) renderQueue.add(h);
        for (CollectablePowerUps p : powerUps) if (p.isActive()) renderQueue.add(p);
        for (Weapons w : weapons) if (w.isActive()) renderQueue.add(w);
        for (Obstacle o : obstacles) renderQueue.add(o);
        renderQueue.add(player);

        renderQueue.sort((a, b) -> {
            // force traps to bottom
            boolean aIsFloor = (a instanceof Traps || a == entry || a == exit);
            boolean bIsFloor = (b instanceof Traps || b == entry || b == exit);

            if (aIsFloor && !bIsFloor) return -1;
            if (bIsFloor && !aIsFloor) return 1;

            return Float.compare(b.getY(), a.getY());
        });

        for (GameObject obj : renderQueue) {
            obj.draw(game.getSpriteBatch());
        }

        for (MageBolt b : bolts)
            b.draw(game.getSpriteBatch());

        game.getSpriteBatch().end();

        // Draw UI
        if (isPaused) {
            // Draw pause menu
            pauseStage.getViewport().apply();
            game.getSpriteBatch().setProjectionMatrix(hud.stage.getCamera().combined);
            game.getSpriteBatch().begin();
            game.getSpriteBatch().draw(blackTexture, 0, 0, hud.stage.getWidth(), hud.stage.getHeight());
            game.getSpriteBatch().end();
            pauseStage.act(delta);
            pauseStage.draw();
        } else {
            // Draw HUD (lives, score, etc.)
            String status = "";
            if (player.isSpeedBoostActive())
                status += "Speed! ";
            if (player.isDamageBoostActive())
                status += "DMG x2! ";

            // Determine what the minimap should point to
            float targetX = player.getX();
            float targetY = player.getY();
            if (key != null && key.isActive()) {
                targetX = key.getX();
                targetY = key.getY();
            } else if (exit != null) {
                targetX = exit.getX();
                targetY = exit.getY();
            }

            hud.stage.getViewport().apply();
            hud.draw(level, isStoryMode, (key != null && !key.isActive()), player.getLives(), status,
                    player.getCurrentStamina(), player.getMaxStamina(), zoomPercentage,
                    player.getX(), player.getY(), targetX, targetY, game);

            hud.drawStatusEffects(player, game);
        }

        // Draw achievement popup
        game.getSpriteBatch().begin();
        game.getSpriteBatch().setProjectionMatrix(hud.stage.getCamera().combined);
        achievementHud.update(delta);
        achievementHud.draw(game.getSpriteBatch(), game.getSkin().getFont("font"));
        game.getSpriteBatch().end();

        // Draw developer console if visible
        if (devConsole != null && devConsole.isVisible()) {
            devConsole.getStage().act(delta);
            devConsole.getStage().draw();
        }
    }

    /**
     * Checks for collisions between the player and items (key, hearts, powerups,
     * exit).
     * Handles pickup logic, score updates, and achievement tracking.
     */
    private void checkEntityCollisions() {
        // Check if player collected the key
        if (key != null && key.isActive() && player.getBounds().overlaps(key.getBounds())) {
            VoiceManager.playCollect("collect_key");
            key.setActive(false);

            VoiceManager.playLevel("door_open");

            // Award points for getting the key
            ScoreManager.getInstance().addBonus("Key Found", 1000);
            hud.addScorePopup("Key +1000", true);
        }

        // Check if player reached the exit
        if (exit != null && exit.isActive() && player.getBounds().overlaps(exit.getBounds())) {
            if (key != null && !key.isActive()) {
                if (!victoryTriggered) {
                    victoryTriggered = true;

                    // finalize the score for this level
                    ScoreManager.getInstance().finishLevel(player.getLives(), levelTimer);

                    if (isStoryMode) {
                        AchievementManager.getInstance().unlock("FINISH_LEVEL_" + level);
                        if (levelTimer < 30f)
                            AchievementManager.getInstance().unlock("SPEEDRUN");
                        if (!tookDamageThisLevel)
                            AchievementManager.getInstance().unlock("NO_DEATH_LEVEL");
                        if (!usedHeartThisLevel)
                            AchievementManager.getInstance().unlock("VEGAN_RUN");
                        if (level == 5) {
                            AchievementManager.getInstance().unlock("STORY_COMPLETE");
                            if (obstacles.size == 0)
                                AchievementManager.getInstance().unlock("LEVEL_5_GENOCIDE");
                        }

                        game.goToVictory();
                    } else {
                        startNewLevel(); // Endless mode continues
                    }
                }
            }
        }

        // Check if player collected hearts
        for (CollectableLives h : hearts) {
            if (h.isActive() && player.getBounds().overlaps(h.getBounds())) {
                h.onCollected(player);
                usedHeartThisLevel = true;

                ScoreManager.getInstance().addBonus("Extra Life", 200);
                hud.addScorePopup("Heart +200", true);

                lastPlayerLives = player.getLives();
            }
        }

        // Check if player collected powerups
        for (CollectablePowerUps p : powerUps) {
            if (p.isActive() && player.getBounds().overlaps(p.getBounds())) {
                p.onCollected(player);
                // granting 5 xp for picking up a powerup
                int xpGained = 5;
                if (MasteryManager.getInstance().addExperience(xpGained)) {
                    hud.showLevelUpNotification();
                }

                ScoreManager.getInstance().addBonus("Powerup", 150);
                hud.addScorePopup("Powerup XP +5", true);
            }

        }

        // Check if player collected weapon upgrades
        for (Weapons w : weapons)
            if (w.isActive() && player.getBounds().overlaps(w.getBounds()))
                w.onCollected(player);
    }

    /**
     * Updates the camera position to follow the player while keeping it within map
     * bounds.
     * Prevents the camera from showing areas outside the map.
     */
    private void updateCamera() {
        float visibleHalfW = (viewport.getWorldWidth() * camera.zoom) / 2f;
        float visibleHalfH = (viewport.getWorldHeight() * camera.zoom) / 2f;
        float mapW = mapManager.getWidth() * mapManager.getTileSize();
        float mapH = mapManager.getHeight() * mapManager.getTileSize();

        float targetX = MathUtils.clamp(player.getX() + 6, visibleHalfW, mapW - visibleHalfW);
        float targetY = MathUtils.clamp(player.getY() + 6, visibleHalfH, mapH - visibleHalfH);

        camera.position.x += (targetX - camera.position.x) * 0.1f;
        camera.position.y += (targetY - camera.position.y) * 0.1f;

        if (shakeTimer > 0) {
            camera.position.x += MathUtils.random(-shakeIntensity, shakeIntensity);
            camera.position.y += MathUtils.random(-shakeIntensity, shakeIntensity);
            shakeTimer -= Gdx.graphics.getDeltaTime();
            shakeIntensity = MathUtils.lerp(shakeIntensity, 0, 0.1f);
        }
        camera.update();
    }

    /**
     * Creates the pause menu UI with buttons for resume, options, save, and quit.
     */
    private void setupPauseMenu() {
        pauseStage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Table table = new Table();
        table.setFillParent(true);
        pauseStage.addActor(table);

        table.add(new Label("Paused", game.getSkin(), "title")).padBottom(40).row();

        // Resume button
        TextButton resume = new TextButton("Resume", game.getSkin());
        resume.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                togglePause();
            }
        });
        table.add(resume).width(300).padBottom(15).row();

        // Options button
        TextButton options = new TextButton("Options", game.getSkin());
        options.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.goToOptions(GameScreen.this);
            }
        });
        table.add(options).width(300).padBottom(15).row();

        // Save and Quit button (only in story mode)
        if (isStoryMode) {
            TextButton saveQuit = new TextButton("Save and Quit", game.getSkin());
            saveQuit.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    VoiceManager.playUI("ui_click");
                    saveCurrentGame();
                    game.goToMenu();
                }
            });
            table.add(saveQuit).width(300).padBottom(15).row();
        }

        // Quit without saving button
        TextButton quit = new TextButton("Quit", game.getSkin());
        quit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_back");
                game.goToMenu();
            }
        });
        table.add(quit).width(300).row();
    }

    /**
     * Developer cheat: instantly gives the player the key.
     */
    public void cheatGetKey() {
        if (key != null) {
            key.setActive(false); // Hide the physical key item
        }
        if (!isStoryMode && exit == null) {
            // Get a guaranteed floor position from the MapManager
            float[] ePos = mapManager.getRandomFloorPosition();

            // Create the exit item using the Tileset Address (EXIT_ROW, EXIT_COL)
            exit = new Item(ePos[0], ePos[1], mapManager.getTileSize(),
                    game.getObjectTexture(EXIT_ROW, EXIT_COL));
        }

        if (exit != null) {
            exit.setActive(true); // Unlock the exit
            Gdx.app.log("Cheat", "Key granted via console! Exit spawned at valid coordinates.");
        }
    }

    /**
     * Developer cheat: instantly kills all enemies on the map.
     */
    public void cheatKillEnemies() {
        obstacles.clear();
    }

    /**
     * Saves the current game state to disk.
     * Only works in story mode and when a save slot is selected.
     */
    private void saveCurrentGame() {
        if (!isStoryMode) {
            Gdx.app.log("SaveSystem", "Cannot save in Endless mode");
            return;
        }

        int currentSlot = game.getCurrentSaveSlot();
        if (currentSlot <= 0) {
            Gdx.app.log("SaveSystem", "No save slot selected");
            return;
        }

        GameStateManager.SaveState state = new GameStateManager.SaveState();

        // Basic info
        state.level = this.level;
        state.isStoryMode = this.isStoryMode;

        // Score
        state.totalScore = ScoreManager.getInstance().getTotalGameScore();
        state.levelBaseScore = ScoreManager.getInstance().getLevelBaseScore();
        state.sessionBonus = ScoreManager.getInstance().getSessionBonus();

        // Player state
        state.playerX = player.getX();
        state.playerY = player.getY();
        state.playerLives = player.getLives();
        state.currentStamina = player.getCurrentStamina();
        state.weaponDamage = player.getWeaponDamage();
        state.timeSinceDeath = 5f; // Prevent death loop

        // Inventory
        state.hasKey = (key == null || !key.isActive());
        state.hasSpeedBoost = player.isSpeedBoostActive();
        state.hasDamageBoost = player.isDamageBoostActive();

        // Enemies
        for (Obstacle o : obstacles) {
            if (o instanceof Enemy) {
                Enemy e = (Enemy) o;
                GameStateManager.EnemyData ed = new GameStateManager.EnemyData();

                if (e instanceof MageEnemy)
                    ed.type = "MAGE";
                else if (e instanceof BigEnemy)
                    ed.type = "BIG";
                else if (e instanceof ChickenJockey)
                    ed.type = "JOCKEY";
                else if (e instanceof NormalEnemy)
                    ed.type = "NORMAL";
                else if (e instanceof ImmortalSnail)
                    continue; // Don't save the snail
                else
                    ed.type = "NORMAL";

                ed.x = e.getX();
                ed.y = e.getY();
                ed.health = ((Enemy) o).isDead() ? 0 : 3; // Default health
                state.enemies.add(ed);
            }
        }

        // Traps
        for (Obstacle o : obstacles) {
            if (o instanceof Traps) {
                Traps t = (Traps) o;
                GameStateManager.TrapData td = new GameStateManager.TrapData();
                td.type = t.getType();
                td.x = t.getX();
                td.y = t.getY();
                state.traps.add(td);
            }
        }

        // Hearts
        for (CollectableLives h : hearts) {
            if (h.isActive()) {
                GameStateManager.ItemData id = new GameStateManager.ItemData();
                id.type = "HEART";
                id.x = h.getX();
                id.y = h.getY();
                id.active = true;
                id.value = 1;
                state.items.add(id);
            }
        }

        // powerups
        for (CollectablePowerUps p : powerUps) {
            if (p.isActive()) {
                GameStateManager.ItemData id = new GameStateManager.ItemData();
                id.type = p.getType().toString();
                id.x = p.getX();
                id.y = p.getY();
                id.active = true;
                id.duration = 5f; // Default duration
                state.items.add(id);
            }
        }

        // Key
        if (key != null) {
            GameStateManager.ItemData keyItem = new GameStateManager.ItemData();
            keyItem.type = "KEY";
            keyItem.x = key.getX();
            keyItem.y = key.getY();
            keyItem.active = key.isActive();
            state.keyData = keyItem;
        }

        // Exit
        if (exit != null) {
            GameStateManager.ItemData exitItem = new GameStateManager.ItemData();
            exitItem.type = "EXIT";
            exitItem.x = exit.getX();
            exitItem.y = exit.getY();
            exitItem.active = exit.isActive();
            state.exitData = exitItem;
        }

        // Save the state
        GameStateManager.saveGame(currentSlot, state);
        Gdx.app.log("SaveSystem", "Game saved successfully to slot " + currentSlot);
    }

    private void loadFromSave(GameStateManager.SaveState state) {
        Gdx.app.log("SaveSystem", "Loading saved game state...");

        // Restore basic info
        this.level = state.level;
        this.isStoryMode = state.isStoryMode;

        // Note: ScoreManager manages its own state during gameplay
        // Scores will be recalculated as the player progresses

        // Load the map for this level
        mapManager.loadLevel(level, isStoryMode);
        float tileSize = mapManager.getTileSize();
        float playerSize = 12;
        float offset = (tileSize - playerSize) / 2f;

        // Create player at saved position
        player = new Player(state.playerX, state.playerY, playerSize, playerSize);
        player.setWeaponDamage(state.weaponDamage);

        // Restore lives
        int diff = state.playerLives - player.getLives();
        if (diff != 0) {
            player.addLives(diff);
        }
        lastPlayerLives = state.playerLives;

        // Restore powerup states
        if (state.hasSpeedBoost)
            player.activateSpeedBoost(999f); // Long duration
        if (state.hasDamageBoost)
            player.activateDamageBoost(999f);

        // Clear and restore enemies
        obstacles.clear();
        for (GameStateManager.EnemyData ed : state.enemies) {
            Enemy enemy = null;
            switch (ed.type) {
                case "NORMAL":
                    enemy = new NormalEnemy(ed.x, ed.y, game);
                    break;
                case "BIG":
                    enemy = new BigEnemy(ed.x, ed.y, game);
                    break;
                case "JOCKEY":
                    enemy = new ChickenJockey(ed.x, ed.y, game);
                    break;
                case "MAGE":
                    enemy = new MageEnemy(ed.x, ed.y, game);
                    break;
            }
            if (enemy != null) {
                obstacles.add(enemy);
            }
        }

        // Restore traps
        for (GameStateManager.TrapData td : state.traps) {
            TextureRegion tex = game.getTrapTexture(td.type);
            obstacles.add(new Traps(td.x, td.y, 16, 16, tex, td.type));
        }

        // Add the immortal snail
        float[] sPos = mapManager.getRandomFloorPosition();
        obstacles.add(new ImmortalSnail(sPos[0], sPos[1], game));

        // Restore items (hearts, powerups)
        hearts.clear();
        powerUps.clear();
        weapons.clear();

        for (GameStateManager.ItemData id : state.items) {
            if (!id.active)
                continue; // Skip inactive items

            switch (id.type) {
                case "HEART":
                    hearts.add(new CollectableLives(id.x, id.y, 16, game.getHeartTexture(), id.value));
                    break;
                case "SPEED":
                    powerUps.add(new CollectablePowerUps(id.x, id.y, 12, game.getFastTexture(),
                            CollectablePowerUps.Type.SPEED, id.duration));
                    break;
                case "DAMAGE":
                    powerUps.add(new CollectablePowerUps(id.x, id.y, 12, game.getObjectTexture(DAMAGE_ROW, DAMAGE_COL),
                            CollectablePowerUps.Type.DAMAGE, id.duration));
                    break;
            }
        }

        // Restore key
        if (state.keyData != null) {
            key = new Item(state.keyData.x, state.keyData.y, 12, game.getKeyTexture());
            key.setActive(state.keyData.active);
        } else {
            // If no key data, create one at default position
            if (isStoryMode && mapManager.getLevelInfo() != null) {
                key = new Item(mapManager.keyX * tileSize + offset, mapManager.keyY * tileSize + offset, 12,
                        game.getKeyTexture());
            }
        }

        // Restore exit
        if (state.exitData != null) {
            exit = new Item(state.exitData.x, state.exitData.y, 16,
                    game.getObjectTexture(EXIT_ROW, EXIT_COL));
            exit.setActive(state.exitData.active);
        } else if (isStoryMode && mapManager.getLevelInfo() != null) {
            exit = new Item(mapManager.exitX * tileSize, mapManager.exitY * tileSize, 16,
                    game.getObjectTexture(EXIT_ROW, EXIT_COL));
            exit.setActive(state.hasKey); // Active if player already has key
        }

        // Reset game state flags
        victoryTriggered = false;
        isLevelCompleting = false;
        completionTimer = 0;
        bolts.clear();

        Gdx.app.log("SaveSystem", "Save loaded successfully - Level " + level);
    }

    /**
     * Toggles the pause menu on/off and switches input handling.
     */
    private void togglePause() {
        isPaused = !isPaused;

        // Notify the main game to duck/unduck the music volume
        game.setInGamePaused(isPaused);

        if (isPaused) {
            pauseCount++;
            if (pauseCount >= 5)
                AchievementManager.getInstance().unlock("PAUSE_SPAM");
            player.clearInput();
        }
        Gdx.input.setInputProcessor(isPaused ? pauseStage : gameInputProcessor);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hud.resize(width, height);
        pauseStage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        // Ensure music is at full volume when screen is shown (entering level)
        game.setInGamePaused(false);
        Gdx.input.setInputProcessor(isPaused ? pauseStage : gameInputProcessor);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        // Ensure we don't leave the music "muffled" if we exit to menu while paused
        game.setInGamePaused(false);
    }

    @Override
    public void dispose() {
        if (background != null)
            background.dispose();
        mapManager.dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    /**
     * Gets the input processor for normal gameplay (used by dev console).
     *
     * @return the input adapter that handles player controls
     */
    public InputAdapter getGameInputProcessor() {
        return gameInputProcessor;
    }
}
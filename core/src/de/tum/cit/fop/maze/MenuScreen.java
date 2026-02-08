package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The main menu screen of the game displayed after the intro animation.
 * Provides navigation to all major game features:
 * - Play (Story or Endless mode selection)
 * - Mastery (Skill tree for permanent upgrades)
 * - Leaderboard (Top 10 endless mode scores)
 * - Achievements (View unlocked achievements)
 * - Options (Settings and controls)
 * - Credits (Game credits - placeholder)
 * - Quit (Exit the game)
 * <p>
 * The menu uses LibGDX Scene2D UI with a Table layout for organizing buttons
 * vertically.
 * Has two states: main menu and play menu (mode selection).
 */
public class MenuScreen implements Screen {

    // Reference to the main game instance for screen navigation and accessing
    // shared resources
    private final MazeRunnerGame game;

    // LibGDX Stage that manages all UI elements and handles input events
    private final Stage stage;

    // Table layout container that organizes all menu buttons vertically
    private final Table table;

    private final com.badlogic.gdx.graphics.Texture background;

    /**
     * Creates the main menu screen with all UI elements.
     * Sets up the stage, loads menu sounds, and displays the main menu.
     *
     * @param game reference to the main game instance
     */
    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        // Create an orthographic camera for 2D UI rendering (no perspective distortion)
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f; // Zoom out slightly to make UI elements appear smaller

        // Use a ScreenViewport so UI scales proportionally with window size
        Viewport viewport = new ScreenViewport(camera);

        // Create the stage using the shared SpriteBatch from the game
        stage = new Stage(viewport, game.getSpriteBatch());

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("menu_background.png"));

        // Create a table to organize menu elements vertically
        table = new Table();
        table.setFillParent(true); // Make table fill the entire screen
        table.center(); // Center all content in the middle of the screen
        stage.addActor(table);

        // Build and display the main menu
        showMainMenu();
    }

    /**
     * Builds and displays the main menu with all primary options.
     * Clears any existing UI and creates fresh menu buttons.
     * Can be called multiple times to reset the menu (e.g., when returning from
     * sub-menus).
     */
    private void showMainMenu() {
        table.clear(); // Remove all existing UI elements

        // Add game title at the top
        table.add(new Label("KNOSSOS", game.getSkin(), "title"))
                .padBottom(50)
                .row();

        // Play button - opens mode selection menu
        TextButton playButton = new TextButton("Play", game.getSkin());
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // REPLACE OLD SOUND LOGIC WITH THIS:
                VoiceManager.playUI("ui_click");
                showPlayMenu();
            }
        });
        table.add(playButton).width(300).padBottom(20).row();

        // Mastery button - opens skill tree screen
        TextButton masteryButton = new TextButton("Mastery", game.getSkin());
        masteryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.setScreen(new MasteryScreen(game, MenuScreen.this));
            }
        });
        table.add(masteryButton).width(300).padBottom(20).row();

        TextButton leaderboardButton = new TextButton("Leaderboard", game.getSkin());
        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.setScreen(new LeaderboardScreen(game));
            }
        });
        table.add(leaderboardButton).width(300).padBottom(20).row();

        // Achievements button
        TextButton achievementButton = new TextButton("Achievements", game.getSkin());
        achievementButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.setScreen(new AchievementScreen(game));
            }
        });
        table.add(achievementButton).width(300).padBottom(20).row();

        // Options button
        TextButton optionsButton = new TextButton("Options", game.getSkin());
        optionsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.goToOptions();
            }
        });
        table.add(optionsButton).width(300).padBottom(20).row();

        // Story button (formerly Credits)
        TextButton storyButton = new TextButton("Story", game.getSkin());
        storyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.setScreen(new StoryScreen(game));
            }
        });
        table.add(storyButton).width(300).padBottom(20).row();

        // Quit button
        TextButton quitButton = new TextButton("Quit", game.getSkin());
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                Gdx.app.exit();
            }
        });
        table.add(quitButton).width(300).row();
    }

    /**
     * Builds and displays the game mode selection menu.
     * Replaces the main menu with options to choose Story or Endless mode.
     * Story mode requires selecting a save slot, endless mode starts immediately.
     */
    private void showPlayMenu() {
        table.clear(); // Remove main menu elements

        // Add mode selection title
        table.add(new Label("Select Mode", game.getSkin(), "title"))
                .padBottom(50)
                .row();

        // Story button
        TextButton storyButton = new TextButton("Story", game.getSkin());
        storyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.setScreen(new SaveSelectionScreen(game));
            }
        });
        table.add(storyButton).width(300).padBottom(20).row();

        // Endless button
        TextButton endlessButton = new TextButton("Endless", game.getSkin());
        endlessButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                game.goToGame(false);
            }
        });
        table.add(endlessButton).width(300).padBottom(20).row();

        // Back button
        TextButton backButton = new TextButton("Back", game.getSkin());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Use the back sound here
                VoiceManager.playUI("ui_back");
                showMainMenu();
            }
        });
        table.add(backButton).width(300).row();
    }

    /**
     * Renders the menu screen every frame.
     * Clears the screen, updates UI logic, and draws all UI elements.
     *
     * @param delta time in seconds since the last frame
     */
    @Override
    public void render(float delta) {
        // Clear the screen to black before drawing UI
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Reset the projection matrix to ensure the background fills the screen
        // (ignoring stage zoom)
        game.getSpriteBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getSpriteBatch().end();

        // Update UI logic (animations, input handling)
        // Limit delta time to prevent large jumps if frame rate drops
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));

        // Draw all UI elements
        stage.draw();
    }

    /**
     * Called when the window is resized.
     * Updates the viewport to keep UI correctly scaled and centered.
     *
     * @param width  new window width in pixels
     * @param height new window height in pixels
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Cleans up all resources used by this screen to prevent memory leaks.
     * Disposes of the stage and sound effects.
     */
    @Override
    public void dispose() {
        if (background != null)
            background.dispose();
        stage.dispose();
    }

    /**
     * Called when this screen becomes the active screen.
     * Sets the stage as the input processor so buttons can receive clicks.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Called when the game is paused (Android/iOS).
     * Not used for desktop menu screen.
     */
    @Override
    public void pause() {
    }

    /**
     * Called when the game is resumed from pause (Android/iOS).
     * Not used for desktop menu screen.
     */
    @Override
    public void resume() {
    }

    /**
     * Called when this screen is no longer active.
     * Input processor is cleared to prevent stale input.
     */
    @Override
    public void hide() {
    }
}

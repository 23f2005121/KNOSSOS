package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * The options/settings menu screen accessible from the main menu or pause menu.
 * It provides a comprehensive game configuration organized into two views
 * <p>
 * All settings persist between game sessions via the preferences system.
 */
public class OptionsMenu implements Screen {

    // Reference to main game for accessing shared resources and screen navigation
    private final MazeRunnerGame game;

    // LibGDX Stage that manages all UI elements and input handling
    private final Stage stage;

    // Table layout container for organizing UI elements vertically
    private final Table table;

    // Persistent preferences for saving/loading settings
    private final GamePreferences prefs;

    // The screen that opened this options menu (MenuScreen, GameScreen, etc.)
    // If null, opened from main menu; otherwise, opened from pause menu
    private final Screen parentScreen;

    private com.badlogic.gdx.graphics.Texture background;

    /**
     * Creates a new options menu screen.
     * Initializes the UI stage and displays the main options view.
     * Remembers the parent screen to return to when the user presses back.
     *
     * @param game         reference to the main game instance
     * @param parentScreen the screen to return to when closing options (null if
     *                     from main menu)
     */
    public OptionsMenu(MazeRunnerGame game, Screen parentScreen) {
        this.game = game;
        this.parentScreen = parentScreen;
        this.prefs = game.getPreferences();

        // Create a stage with a ScreenViewport for automatic UI scaling
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("options_background.png"));

        // Create a table to organize UI elements
        this.table = new Table();
        table.setFillParent(true); // Fill the entire screen
        stage.addActor(table);

        // Build and display the main options menu
        showMainOptions();
    }

    /**
     * Builds and displays the main options menu with audio and control settings.
     * This is the default view shown when the options menu is opened.
     *
     * Contains:
     * - Music volume slider with live preview
     * - Sound effects (SFX) volume slider
     * - Button to access keyboard configuration
     * - Back button to return to previous screen
     *
     * All changes are applied immediately and saved to preferences.
     */
    private void showMainOptions() {
        table.clear(); // Remove any existing UI elements

        // Add "Options" title at the top
        table.add(new Label("Options", game.getSkin(), "title"))
                .padBottom(40)
                .row();

        // Music Volume Control Section
        table.add(new Label("Music Volume", game.getSkin()))
                .padBottom(10)
                .row();

        // Create slider with range 0.0 to 1.0 (0% to 100%), step size 0.1 (10%)
        final Slider volumeSlider = new Slider(0f, 1f, 0.1f, false, game.getSkin());
        volumeSlider.setValue(prefs.getMusicVolume());

        // Update music volume in real-time as slider moves
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float val = volumeSlider.getValue();
                prefs.setMusicVolume(val); // Save to preferences
                game.setMusicVolume(val); // Apply immediately to playing music
            }
        });

        table.add(volumeSlider)
                .width(300)
                .padBottom(20)
                .row();

        // SFX Volume Control Section
        table.add(new Label("SFX Volume", game.getSkin()))
                .padBottom(10)
                .row();

        // Create slider for sound effects with range 0.0 to 1.0
        final Slider sfxSlider = new Slider(0f, 1f, 0.1f, false, game.getSkin());
        sfxSlider.setValue(prefs.getSoundVolume());

        // Update SFX volume in real-time
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float val = sfxSlider.getValue();
                prefs.setSoundVolume(val); // Save to preferences
                VoiceManager.setVolume(val); // Apply to sound manager
            }
        });

        table.add(sfxSlider)
                .width(300)
                .padBottom(30)
                .row();

        // Keyboard Configuration Button
        // Opens a separate screen for rebinding all keys
        TextButton keyConfigBtn = new TextButton("Keyboard Configuration", game.getSkin());

        // Make text wrap and scale down to fit in button without overflow
        keyConfigBtn.getLabel().setWrap(true);
        keyConfigBtn.getLabel().setFontScale(0.8f); // Scale to 80% size
        keyConfigBtn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);

        // Set wrap width slightly smaller than button width for internal padding
        keyConfigBtn.getLabelCell().width(240).pad(5);

        keyConfigBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                showKeyConfig(); // Switch to keyboard configuration view
            }
        });

        table.add(keyConfigBtn)
                .width(300)
                .height(80) // Taller button to accommodate two lines of text
                .padBottom(20)
                .row();

        // Back Button
        // Returns to parent screen (main menu or paused game)
        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_back");
                if (parentScreen != null) {
                    // Return to paused game screen
                    game.setScreen(parentScreen);
                    dispose(); // Clean up options menu resources
                } else {
                    // Return to main menu
                    game.goToMenu();
                }
            }
        });

        table.add(backBtn)
                .width(300)
                .row();
    }

    /**
     * Builds and displays the keyboard configuration menu.
     * Shows a scrollable list of all rebindable actions with their current keys.
     * <p>
     * Also unlocks the "SETTINGS_CHANGED" achievement when any key is rebound.
     */
    private void showKeyConfig() {
        table.clear();

        // Add title and instructions
        table.add(new Label("Key Configuration", game.getSkin(), "title"))
                .padBottom(10)
                .row();

        table.add(new Label("Click a button, then press a key", game.getSkin()))
                .padBottom(10)
                .row();

        // Create inner table for scrollable key bindings list
        Table innerTable = new Table();

        // Create a row for each rebindable action
        // Each row has: [Action Name Label] [Current Key Button]
        createKeyRow(innerTable, "Move Up", prefs.getKeyUp(), key -> prefs.setKeyUp(key));
        createKeyRow(innerTable, "Move Down", prefs.getKeyDown(), key -> prefs.setKeyDown(key));
        createKeyRow(innerTable, "Move Left", prefs.getKeyLeft(), key -> prefs.setKeyLeft(key));
        createKeyRow(innerTable, "Move Right", prefs.getKeyRight(), key -> prefs.setKeyRight(key));
        createKeyRow(innerTable, "Sprint", prefs.getKeySprint(), key -> prefs.setKeySprint(key));
        createKeyRow(innerTable, "Dash", prefs.getKeyDash(), key -> prefs.setKeyDash(key));
        createKeyRow(innerTable, "Attack", prefs.getKeyAttack(), key -> prefs.setKeyAttack(key));
        createKeyRow(innerTable, "Zoom In", prefs.getKeyZoomIn(), key -> prefs.setKeyZoomIn(key));
        createKeyRow(innerTable, "Zoom Out", prefs.getKeyZoomOut(), key -> prefs.setKeyZoomOut(key));
        createKeyRow(innerTable, "Toggle Fullscreen", prefs.getKeyFullscreen(), key -> prefs.setKeyFullscreen(key));
        createKeyRow(innerTable, "Dev Console", prefs.getKeyConsole(), key -> prefs.setKeyConsole(key));

        // Wrap inner table in a scroll pane for easy access to all bindings
        ScrollPane scrollPane = new ScrollPane(innerTable, game.getSkin());
        scrollPane.setFadeScrollBars(false); // Always show scrollbars
        table.add(scrollPane).expand().fill().pad(10).row();

        // Back button returns to main options menu
        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_back");
                showMainOptions(); // Return to main options view
            }
        });

        table.add(backBtn)
                .width(300)
                .padTop(10)
                .padBottom(10)
                .row();
    }

    /**
     * Creates a single row in the key configuration menu.
     * Each row displays an action name and a button showing the current key.
     * Clicking the button enters rebind mode
     *
     * @param targetTable the table to add this row to
     * @param actionName  display name of the action (e.g., "Move Up")
     * @param currentKey  the keycode currently bound to this action
     * @param setter      callback interface to save the new keycode to preferences
     */
    private void createKeyRow(Table targetTable, String actionName, int currentKey, KeySetter setter) {
        // Add action name label on the left
        targetTable.add(new Label(actionName, game.getSkin())).padRight(20).left();

        // Get human-readable name for the current key (e.g., "W", "SPACE")
        String keyName = Input.Keys.toString(currentKey);
        final TextButton keyButton = new TextButton(keyName, game.getSkin());

        // When button is clicked, enter rebind mode
        keyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                keyButton.setText("Press Key..."); // Indicate waiting for input

                // Replace input processor with a temporary one that captures the next key
                Gdx.input.setInputProcessor(new InputAdapter() {
                    @Override
                    public boolean keyDown(int keycode) {
                        setter.setKey(keycode); // Save new key to preferences
                        AchievementManager.getInstance().unlock("SETTINGS_CHANGED"); // Unlock achievement
                        keyButton.setText(Input.Keys.toString(keycode)); // Update button text
                        Gdx.input.setInputProcessor(stage); // Restore normal input handling
                        return true; // Consume the event
                    }
                });
            }
        });

        // Add key button on the right
        targetTable.add(keyButton).width(150).padBottom(5).row();
    }

    /**
     * Functional interface for saving a new key binding.
     * Used as a callback when the user rebinds a key.
     * Each action passes a lambda that calls the appropriate setter in
     * GamePreferences.
     */
    interface KeySetter {
        /**
         * Saves the new keycode to preferences.
         * 
         * @param keycode the LibGDX keycode to assign (e.g., Input.Keys.W)
         */
        void setKey(int keycode);
    }

    /**
     * Renders the options menu every frame.
     * Clears the screen to dark gray and draws all UI elements.
     *
     * @param delta time in seconds since the last frame
     */
    @Override
    public void render(float delta) {
        // Clear screen to dark gray background
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getSpriteBatch().end();

        // Update UI logic (animations, input)
        stage.act(delta);

        // Draw all UI elements
        stage.draw();
    }

    /**
     * Called when the window is resized.
     * Updates the viewport to maintain correct UI scaling.
     *
     * @param width  new window width in pixels
     * @param height new window height in pixels
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Called when this screen becomes active.
     * Sets the stage as the input processor so UI elements receive input.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Called when this screen is no longer active.
     * Clears the input processor to prevent stale input handling.
     */
    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    /**
     * Cleans up all resources used by this screen.
     * Called when returning to the parent screen or main menu.
     */
    @Override
    public void dispose() {
        if (background != null)
            background.dispose();
        stage.dispose();
    }

    /**
     * Called when the game is paused (Android/iOS).
     * Not used for desktop options' menu.
     */
    @Override
    public void pause() {
    }

    /**
     * Called when the game is resumed from pause (Android/iOS).
     * Not used for desktop options' menu.
     */
    @Override
    public void resume() {
    }
}

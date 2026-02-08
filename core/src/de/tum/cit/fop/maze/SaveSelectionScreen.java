package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Save slot selection screen for story mode.
 * Provides 4 independent save slots, allowing multiple players or playthroughs.
 * 
 * Endless mode does not use save slots
 */
public class SaveSelectionScreen implements Screen {

    // Reference to main game for screen navigation and shared resources
    private final MazeRunnerGame game;

    // LibGDX Stage that manages all UI elements
    private final Stage stage;

    // Table layout container for organizing save slot rows
    private final Table table;

    private final com.badlogic.gdx.graphics.Texture background;

    /**
     * Creates the save selection screen with 4 save slots.
     * Checks which slots have existing save files and builds the UI accordingly.
     *
     * @param game reference to the main game instance
     */
    public SaveSelectionScreen(MazeRunnerGame game) {
        this.game = game;

        // Create stage with automatic scaling
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("save_selection_background.png"));

        // Create table for save slot layout
        table = new Table();
        table.setFillParent(true); // Fill entire screen
        stage.addActor(table);

        // Build the UI showing all save slots
        rebuildUi();
    }

    /**
     * Builds or rebuilds the save slot selection UI.
     * Creates a row for each of the 4 save slots
     * The UI is rebuilt dynamically after a save is deleted to update button
     * states.
     *
     */
    private void rebuildUi() {
        table.clear(); // Remove all existing UI elements

        // Add title spanning all 3 columns
        table.add(new Label("Select Save File", game.getSkin(), "title"))
                .colspan(3)
                .padBottom(50)
                .row();

        // Create a row for each of the 4 save slots
        for (int i = 1; i <= 4; i++) {
            final int slotIndex = i; // Final copy for use in listeners

            // Check if this slot has an existing save file
            String fileName = "slot" + i + ".json";
            boolean saveExists = Gdx.files.local(fileName).exists();

            // ===== COLUMN 1: Slot Number Label =====
            table.add(new Label("Slot " + i, game.getSkin())).pad(10);

            // ===== COLUMN 2: Action Button (Continue or New Game) =====
            // Button text changes based on whether save file exists
            String actionText = saveExists ? "Continue" : "New Game";
            TextButton actionBtn = new TextButton(actionText, game.getSkin());

            actionBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    VoiceManager.playUI("ui_click");

                    // Set the active save slot so progress saves to correct file
                    game.setCurrentSaveSlot(slotIndex);

                    if (saveExists) {
                        // CONTINUE: Load existing save
                        // GameScreen will detect the save file and load progress
                        game.goToGame(true); // true = story mode
                    } else {
                        // NEW GAME: Start fresh from level 1
                        // Wipes any in-memory progress and starts story mode
                        game.restartStory();
                    }
                }
            });
            table.add(actionBtn).width(200).pad(10);

            // ===== COLUMN 3: Reset Button =====
            // Deletes the save file for this slot
            TextButton resetBtn = new TextButton("Reset", game.getSkin());

            if (!saveExists) {
                // Gray out and disable if no save exists
                resetBtn.setDisabled(true);
                resetBtn.setColor(1, 1, 1, 0.5f); // 50% opacity
            } else {
                // Enable reset functionality for existing saves
                resetBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        VoiceManager.playUI("ui_click");

                        // Delete the save file using GameStateManager
                        GameStateManager.deleteSave(slotIndex);

                        Gdx.app.log("SaveSystem", "Deleted save slot " + slotIndex);

                        // Rebuild UI to update button states
                        // Reset button will now be grayed out
                        // Action button will now say "New Game"
                        rebuildUi();
                    }
                });
            }
            table.add(resetBtn).width(150).pad(10).row();
        }

        // ===== BACK BUTTON =====
        // Returns to the play menu (Story/Endless selection)
        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_back");
                game.goToMenu(); // Return to main menu
            }
        });
        table.add(backBtn)
                .colspan(3) // Span all 3 columns
                .width(200)
                .padTop(40)
                .row();
    }

    /**
     * Renders the save selection screen every frame.
     * Clears to black background and draws all UI elements.
     *
     * @param delta time in seconds since the last frame
     */
    @Override
    public void render(float delta) {
        // Clear screen to black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getSpriteBatch().end();

        // Update and draw UI
        stage.act(delta);
        stage.draw();
    }

    /**
     * Called when this screen becomes active.
     * Sets the stage as the input processor so buttons receive clicks.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
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
     * Called when the game is paused (Android/iOS).
     * Not used for desktop save selection screen.
     */
    @Override
    public void pause() {
    }

    /**
     * Called when the game is resumed from pause (Android/iOS).
     * Not used for desktop save selection screen.
     */
    @Override
    public void resume() {
    }

    /**
     * Called when this screen is no longer active.
     * Clears input processor to prevent stale input.
     */
    @Override
    public void hide() {
    }

    /**
     * Cleans up all resources used by this screen.
     * Called when navigating to a different screen.
     */
    @Override
    public void dispose() {
        if (background != null)
            background.dispose();
        stage.dispose();
    }
}

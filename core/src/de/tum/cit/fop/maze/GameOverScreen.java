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
 * Screen displayed when the player runs out of lives and fails to complete the
 * level.
 * Shows a "Game Over" message with options to retry the same mode or return to
 * the main menu.
 */
public class GameOverScreen implements Screen {

    // Reference to the main game for navigation and accessing assets
    private final MazeRunnerGame game;

    // LibGDX Stage that holds all the UI elements (buttons and labels)
    private Stage stage;

    // Tracks whether the player was in Story Mode or Endless Mode when they died
    private boolean wasStoryMode;

    private com.badlogic.gdx.graphics.Texture background;

    /**
     * Creates the Game Over screen with retry and menu navigation options.
     * Plays a game over sound effect and builds the UI layout.
     *
     * @param game         reference to the main game instance
     * @param wasStoryMode true if the player was in Story Mode, false if in Endless
     *                     Mode
     */
    public GameOverScreen(MazeRunnerGame game, boolean wasStoryMode) {
        this.game = game;
        this.wasStoryMode = wasStoryMode;

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("game_over_background.png"));

        try {
            // Try to create the stage with the game's sprite batch
            this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
            Table table = new Table();
            table.setFillParent(true);
            stage.addActor(table);

            // Attempt to load the title label style from the skin
            Label.LabelStyle style = game.getSkin().get("title", Label.LabelStyle.class);
            if (style == null)
                style = game.getSkin().get(Label.LabelStyle.class); // Fallback to default style
        } catch (Exception e) {
            // If anything fails, create an empty stage to prevent the game from crashing
            this.stage = new Stage(new ScreenViewport());
        }

        // Create the main layout table that organizes all UI elements vertically
        Table table = new Table();
        table.setFillParent(true); // Make the table fill the entire screen
        stage.addActor(table);

        // Play the game over announcement sound
        VoiceManager.playAnnouncer("game_over");

        // Add the "GAME OVER" title at the top
        table.add(new Label("GAME OVER", game.getSkin(), "title"))
                .padBottom(40)
                .row();

        // Create the Retry button
        TextButton retryButton = new TextButton("Retry", game.getSkin());
        retryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Restart the appropriate game mode based on what the player was playing
                if (wasStoryMode) {
                    game.restartStory(); // Restart from level 1 in Story Mode
                } else {
                    game.goToGame(false); // Start a new Endless Mode run
                }
            }
        });
        table.add(retryButton)
                .width(300)
                .padBottom(20)
                .row();

        // Create the Back to Menu button
        TextButton menuButton = new TextButton("Back to Menu", game.getSkin());
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Return to the main menu
                game.goToMenu();
            }
        });
        table.add(menuButton).width(300);
    }

    /**
     * Called when this screen becomes the active screen.
     * Sets the stage as the input processor so buttons can be clicked.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Renders the screen every frame.
     * Clears the screen with a black background and draws all UI elements.
     *
     * @param delta time passed since last frame in seconds
     */
    @Override
    public void render(float delta) {
        // Clear the screen with black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getSpriteBatch().end();

        // Update and draw all UI elements on the stage
        stage.act(delta);
        stage.draw();
    }

    /**
     * Called when the window is resized.
     * Updates the stage viewport to match the new window dimensions.
     *
     * @param width  new window width
     * @param height new window height
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    /**
     * Cleans up resources when the screen is no longer needed.
     */
    @Override
    public void dispose() {
        if (background != null)
            background.dispose();
        stage.dispose();
    }
}

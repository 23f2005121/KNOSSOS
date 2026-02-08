package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.Map;

/**
 * Victory screen shown when the player successfully completes a level.
 * Displays a detailed score breakdown and provides navigation options.
 * <p>
 *
 *
 * The score breakdown helps players understand their performance
 * and encourages replay for better scores (speedrunning, no damage, etc.)
 */
public class VictoryScreen implements Screen {
    // Reference to main game for screen navigation
    private final MazeRunnerGame game;

    // LibGDX Stage for UI rendering and input handling
    private final Stage stage;

    private com.badlogic.gdx.graphics.Texture background;

    /**
     * Creates the victory screen with score breakdown and navigation buttons.
     * Reads the current score breakdown from ScoreManager and displays it.
     * <p>
     * The screen layout is built dynamically based on:
     * - Score breakdown entries (variable number of items)
     * - Story completion status (affects title and button visibility)
     *
     * @param game reference to the main game instance
     */
    public VictoryScreen(MazeRunnerGame game) {
        this.game = game;

        // Create stage for UI rendering
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("victory_background.png"));

        // Play victory announcer sound effect
        VoiceManager.playAnnouncer("victory");

        // Create root table that fills the entire screen
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Get score breakdown from ScoreManager
        ScoreManager sm = ScoreManager.getInstance();

        // ===== DETERMINE IF STORY IS COMPLETED =====
        // Story has 5 levels (1-5). If on level 5 or beyond, story is complete
        boolean isStoryFinished = (game.getCurrentStoryLevel() >= 5);

        // ===== TITLE =====
        // Different title for regular victory vs story completion
        String titleText = isStoryFinished ? "STORY COMPLETED!" : "LEVEL COMPLETE!";
        root.add(new Label(titleText, game.getSkin(), "title"))
                .padBottom(50)
                .row();

        // --- NEW: Generate a semi-transparent background for the score card ---
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.65f); // 65% opacity black
        pixmap.fill();
        TextureRegionDrawable cardBg = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();

        // ===== SCORE BREAKDOWN TABLE =====
        // Create inner table for score breakdown entries
        Table scoreTable = new Table();
        scoreTable.setBackground(cardBg); // Apply the dark panel background
        scoreTable.defaults().pad(10, 40, 10, 40); // Generous padding inside the card

        // Loop through all score components from ScoreManager
        // LinkedHashMap preserves insertion order for consistent display
        for (Map.Entry<String, Integer> entry : sm.getBreakdown().entrySet()) {
            // Create label for score component name (left-aligned)
            Label nameLabel = new Label(entry.getKey(), game.getSkin());

            // Create label for score value (right-aligned)
            // Add + prefix for positive scores (negative already has -)
            Label valLabel = new Label((entry.getValue() >= 0 ? "+" : "") + entry.getValue(), game.getSkin());

            // Color coding for visual feedback
            // Green = bonuses/good performance
            // Red = penalties/mistakes
            valLabel.setColor(entry.getValue() >= 0 ? Color.GREEN : Color.RED);

            // Add to score table with fixed widths for alignment
            // 300px for name (left-aligned)
            // 100px for value (right-aligned)
            // INCREASED WIDTHS: 400px and 150px for more horizontal spacing
            scoreTable.add(nameLabel).left().width(400);
            scoreTable.add(valLabel).right().width(150);
            scoreTable.row(); // Move to next row
        }

        // Add a small divider/padding before the total
        scoreTable.add().height(20).row();

        // Add score table to root layout
        root.add(scoreTable).padBottom(40).row();

        // ===== TOTAL SCORE =====
        // Display large, gold-colored total score
        Label totalLabel = new Label("TOTAL SCORE: " + sm.getLevelTotal(), game.getSkin(), "title");
        totalLabel.setFontScale(0.85f); // Slightly smaller than main title
        totalLabel.setColor(Color.GOLD); // Gold color for emphasis
        root.add(totalLabel).padBottom(60).row();

        // ===== NAVIGATION BUTTONS =====

        // Next Level button (only shown if story is not finished)
        if (!isStoryFinished) {
            TextButton nextBtn = new TextButton("Next Level", game.getSkin());
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    VoiceManager.playUI("ui_click");
                    // Advance to next story level
                    game.goToNextStoryLevel();
                }
            });
            root.add(nextBtn).width(350).height(60).padBottom(15).row();
        }
        // If story is finished (level 5), no next button appears
        // Player must return to menu to start new game or play endless

        // Back to Menu button (always shown)
        TextButton menuBtn = new TextButton("Back to Menu", game.getSkin());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                // Return to main menu
                game.goToMenu();
            }
        });
        root.add(menuBtn).width(350).height(60);
    }

    /**
     * Renders the victory screen every frame.
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
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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

    /**
     * Called when the game is paused (Android/iOS).
     * Not used for desktop victory screen.
     */
    @Override
    public void pause() {
    }

    /**
     * Called when the game is resumed from pause (Android/iOS).
     * Not used for desktop victory screen.
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
}
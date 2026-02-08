package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Screen that displays all achievements in a polished list of boxes.
 * Uses a generated background texture to create "Cards" for each entry.
 */
public class AchievementScreen implements Screen {
    // Reference to the main game object for accessing assets and navigation
    private final MazeRunnerGame game;

    // LibGDX Stage that holds all the UI elements (buttons, labels, scroll panes)
    private final Stage stage;

    // The main container table that holds the title, list, and buttons
    private final Table mainTable;

    // Reusable background texture for achievement cards (tinted for
    // unlocked/locked)
    private TextureRegionDrawable cardBackground;

    private Texture background;

    /**
     * Constructor that sets up the achievement screen layout.
     *
     * @param game reference to the main game instance
     */
    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("achievements_background.png"));

        // Create a 1x1 white pixel texture to use as a flexible background
        createBackgroundDrawable();

        mainTable = new Table();
        mainTable.setFillParent(true); // Makes the table fill the entire screen
        stage.addActor(mainTable);

        buildUI(); // Build all the UI elements (title, list, buttons)
    }

    /**
     * Creates a simple white texture that we can tint to any color
     * to use as backgrounds for our achievement boxes.
     */
    private void createBackgroundDrawable() {
        // Create a 1x1 pixel image (Pixmap) filled with white
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        // Convert it to a drawable so we can use it as a background
        cardBackground = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose(); // Clean up the pixmap to free memory
    }

    /**
     * Builds the entire UI structure: title, scrollable achievement list, and
     * buttons.
     * Called when the screen is first created and when achievements are reset.
     */
    private void buildUI() {
        mainTable.clear(); // Remove any old content if rebuilding

        // 1. Title at the top
        Label titleLabel = new Label("Achievements", game.getSkin(), "title");
        mainTable.add(titleLabel).padTop(30).padBottom(20).row();

        // 2. The Scrollable List of achievements
        Table scrollTable = new Table();
        scrollTable.top(); // Align content to the top of the scroll area
        scrollTable.defaults().pad(10).fillX(); // Make boxes stretch to full width

        // Loop through every achievement and create a card for it
        for (AchievementManager.AchievementEntry a : AchievementManager.getInstance().getAllAchievements()) {
            boolean isUnlocked = AchievementManager.getInstance().isUnlocked(a.id);

            // --- CREATE AN ACHIEVEMENT CARD (BOX) ---
            Table card = new Table();
            card.setBackground(cardBackground); // Use the white pixel as background

            // Tint the box: Dark Green if unlocked, Dark Grey if locked
            if (isUnlocked) {
                card.setColor(0.1f, 0.25f, 0.1f, 1f); // Subtle Green tint
            } else {
                card.setColor(0.15f, 0.15f, 0.15f, 1f); // Subtle Grey tint
            }

            card.defaults().left().padLeft(20).padRight(20); // Left-align content with padding

            // Title inside card
            Label aTitle = new Label(a.title, game.getSkin());
            aTitle.setColor(isUnlocked ? Color.GREEN : Color.GRAY);
            card.add(aTitle).padTop(15).row();

            // Description inside card
            Label aDesc = new Label(a.desc, game.getSkin());
            aDesc.setColor(isUnlocked ? Color.WHITE : Color.LIGHT_GRAY);
            aDesc.setWrap(true); // Allow text to wrap if it's too long
            card.add(aDesc).width(650).padBottom(15).row();

            // Add the finished card to the main list with a gap below it
            scrollTable.add(card).padBottom(10).row();
        }

        // Wrap the list in a scroll pane so you can scroll through many achievements
        ScrollPane scrollPane = new ScrollPane(scrollTable, game.getSkin());
        scrollPane.setFadeScrollBars(false); // Keep scrollbars always visible
        scrollPane.setScrollingDisabled(true, false); // Disable horizontal scroll, enable vertical

        mainTable.add(scrollPane).expand().fill().pad(20).row();

        // 3. Buttons at the bottom (Back and Reset)
        Table buttonTable = new Table();

        // Back button to return to main menu
        TextButton backButton = new TextButton("Back", game.getSkin());
        backButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                VoiceManager.playUI("ui_back"); // Play sound effect
                game.goToMenu(); // Navigate back to the main menu
            }
        });

        // Reset button to clear all achievement progress
        TextButton resetButton = new TextButton("Reset All", game.getSkin());
        resetButton.getLabel().setColor(Color.RED); // Red text to indicate danger
        resetButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                VoiceManager.playUI("ui_click");
                AchievementManager.getInstance().resetAchievements(); // Clear all progress
                buildUI(); // Rebuild the UI to reflect the reset state
            }
        });

        buttonTable.add(backButton).width(200).pad(10);
        buttonTable.add(resetButton).width(200).pad(10);

        mainTable.add(buttonTable).padBottom(30);
    }

    /**
     * Called every frame to update and draw the screen.
     *
     * @param delta time passed since last frame in seconds
     */
    @Override
    public void render(float delta) {
        // Clear the screen with a black background
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
     * Called when this screen becomes the active screen.
     * Sets the stage as the input processor so buttons work.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Called when the window is resized.
     * Updates the stage viewport to match the new dimensions.
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Called when leaving this screen. Clears input processing.
     */
    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
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

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }
}

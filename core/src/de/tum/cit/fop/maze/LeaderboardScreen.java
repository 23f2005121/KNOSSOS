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
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Screen that displays the top 10 high scores from endless mode.
 * Shows a table with rank, score, and max level reached for each entry.
 * The #1 entry gets special gold styling to stand out.
 * <p>
 * Uses a fixed-width table layout with vertical separators between columns
 * for a clean, organized appearance.
 */
public class LeaderboardScreen implements Screen {

    // Reference to the main game for accessing assets and navigation
    private final MazeRunnerGame game;

    // LibGDX Stage that holds all UI elements
    private final Stage stage;

    // Main container table that organizes the title, header, list, and back button
    private final Table mainTable;

    // Reusable white texture that can be tinted for backgrounds and separators
    private TextureRegionDrawable cardBackground;

    private Texture background;

    // Fixed column widths for the leaderboard table (keeps everything aligned)
    private static final float RANK_W = 120f; // Width of the rank column (#1, #2, etc.)
    private static final float SCORE_W = 380f; // Width of the score column
    private static final float LEVEL_W = 240f; // Width of the level column
    private static final float SEP_W = 2f; // Width of vertical separator lines
    private static final float TOTAL_WIDTH = RANK_W + SCORE_W + LEVEL_W + (SEP_W * 2); // Total table width

    /**
     * Creates the leaderboard screen and builds the UI layout.
     *
     * @param game reference to the main game instance
     */
    public LeaderboardScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("leaderboard_background.png"));

        createBackgroundDrawable();
        mainTable = new Table();
        mainTable.setFillParent(true); // Make table fill the entire screen
        stage.addActor(mainTable);
        buildUI(); // Build all the UI elements
    }

    /**
     * Creates a simple white 1x1 pixel texture that can be tinted to any color.
     * Used for card backgrounds and separator lines.
     */
    private void createBackgroundDrawable() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        cardBackground = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose(); // Clean up to free memory
    }

    /**
     * Creates a thin vertical line separator with the specified color.
     * Used to separate columns in the leaderboard table.
     *
     * @param color the color to tint the separator
     * @return an Image widget configured as a separator line
     */
    private Image createSeparator(Color color) {
        Image img = new Image(cardBackground);
        img.setColor(color);
        return img;
    }

    /**
     * Builds the entire UI structure: title, column headers, scrollable score list,
     * and back button.
     * The #1 ranked entry gets special gold styling to highlight it.
     */
    private void buildUI() {
        mainTable.clear(); // Remove any old content
        mainTable.top(); // Align content to the top

        // 1. TITLE SECTION
        Label titleLabel = new Label("HALL OF FAME", game.getSkin(), "title");
        mainTable.add(titleLabel).padTop(30).padBottom(10).row();

        Label subTitle = new Label("Top 10 Endless Mode Runs", game.getSkin());
        subTitle.setColor(Color.GRAY);
        mainTable.add(subTitle).padBottom(30).row();

        // 2. FIXED HEADER SECTION (column labels)
        Table headerTable = new Table();
        headerTable.setBackground(cardBackground);
        headerTable.setColor(0.1f, 0.1f, 0.1f, 1f); // Dark gray background

        // Create column header labels
        Label hRank = new Label(" Rank", game.getSkin());
        Label hScore = new Label("  Score", game.getSkin());
        Label hLvl = new Label("  Max Level", game.getSkin());

        // Add headers with fixed widths and separators between them
        headerTable.add(hRank).width(RANK_W).center();
        headerTable.add(createSeparator(Color.GRAY)).width(SEP_W).fillY();
        headerTable.add(hScore).width(SCORE_W).center();
        headerTable.add(createSeparator(Color.GRAY)).width(SEP_W).fillY();
        headerTable.add(hLvl).width(LEVEL_W).center();

        mainTable.add(headerTable).width(TOTAL_WIDTH).height(50).center().row();

        // 3. SCROLLABLE LIST SECTION (the actual leaderboard entries)
        Table scrollTable = new Table();
        scrollTable.top(); // Align entries to the top of the scroll area

        int rank = 1;
        // Loop through all high scores and create a row for each
        for (LeaderboardManager.ScoreEntry entry : LeaderboardManager.getInstance().getHighScores()) {

            // Create a card (row) for this entry
            Table card = new Table();
            card.pad(0);
            card.setBackground(cardBackground);
            Color lineColor;

            // Special gold styling for rank #1
            if (rank == 1) {
                card.setColor(0.35f, 0.3f, 0.05f, 1f); // Dark gold background
                lineColor = Color.GOLD;
            } else {
                card.setColor(0.18f, 0.18f, 0.18f, 1f); // Dark gray for other ranks
                lineColor = Color.DARK_GRAY;
            }

            // Rank Column
            Label rankLbl = new Label(" #" + rank, game.getSkin());
            rankLbl.setColor(rank == 1 ? Color.GOLD : Color.LIGHT_GRAY);
            card.add(rankLbl).width(RANK_W).center();

            card.add(createSeparator(lineColor)).width(SEP_W).fillY().pad(0);

            // Score Column
            Label scoreLbl = new Label(" " + entry.score, game.getSkin(), "title");
            scoreLbl.setFontScale(0.55f); // Slightly smaller font
            scoreLbl.setColor(rank == 1 ? Color.GOLD : Color.WHITE);
            card.add(scoreLbl).width(SCORE_W).center();

            card.add(createSeparator(lineColor)).width(SEP_W).fillY().pad(0);

            // Level Column
            Label levelLbl = new Label("  " + entry.levelsCleared, game.getSkin());
            levelLbl.setColor(rank == 1 ? Color.GOLD : Color.WHITE);
            card.add(levelLbl).width(LEVEL_W).center();

            // Add the completed row to the scrollable list
            scrollTable.add(card).width(TOTAL_WIDTH).height(65).padBottom(4).center().row();
            rank++;
        }

        // Wrap the list in a scroll pane so you can scroll if there are many entries
        ScrollPane scrollPane = new ScrollPane(scrollTable, game.getSkin());
        scrollPane.setFadeScrollBars(false); // Keep scrollbar always visible
        scrollPane.setOverscroll(false, false); // Prevent over-scrolling past edges

        // Add the scroll pane to the main table, allowing it to expand and fill
        // vertical space
        mainTable.add(scrollPane).width(TOTAL_WIDTH).expand().fill().center().padBottom(10).row();

        // 4. BACK BUTTON SECTION
        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_back");
                game.goToMenu(); // Return to main menu
            }
        });

        // Add the back button at the bottom
        mainTable.add(backBtn).width(350).height(60).padBottom(30).center();
    }

    /**
     * Renders the leaderboard screen every frame.
     * Clears the screen to black and draws all UI elements.
     *
     * @param delta time passed since last frame in seconds
     */
    @Override
    public void render(float delta) {
        // Clear screen to black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getSpriteBatch().end();

        // Update and draw all UI elements
        stage.act(delta); // Important: makes scrolling and buttons work
        stage.draw();
    }

    /**
     * Called when this screen becomes the active screen.
     * Sets the stage as the input processor so buttons and scrolling work.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Called when the window is resized.
     * Updates the stage viewport to match the new dimensions.
     *
     * @param width  new window width
     * @param height new window height
     */
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

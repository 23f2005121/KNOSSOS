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
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Screen that displays the skill tree where players can spend skill points on
 * permanent upgrades.
 * Shows a 4x4 grid of upgrades organized into four categories:
 * - Speed: Movement speed and stamina improvements
 * - Dash: Dash cooldown reduction and invincibility
 * - Attack: Damage and attack speed increases
 * - Health: Life regeneration improvements
 * <p>
 * Each category has 4 tiers that must be unlocked in order (can't buy tier 2
 * without tier 1).
 * Visual feedback:
 * - Green boxes = owned
 * - Yellow boxes = available to purchase (previous tier unlocked)
 * - Gray boxes = locked (previous tier not owned yet)
 * <p>
 * Clicking a box shows its details and purchase button in a panel below the
 * grid.
 */
public class MasteryScreen implements Screen {

    // Reference to the main game for accessing assets and navigation
    private final MazeRunnerGame game;

    // The screen to return to when the player presses "Back"
    private final Screen parentScreen;

    // LibGDX Stage that holds all UI elements
    private final Stage stage;

    // Reference to the mastery manager for checking/unlocking upgrades
    private final MasteryManager manager;

    // UI Elements that need to be updated dynamically
    private Label pointsLabel; // Shows current skill points and XP
    private Label detailNameLabel; // Shows name of selected upgrade
    private Label detailDescLabel; // Shows description of selected upgrade
    private TextButton purchaseButton; // Button to buy the selected upgrade
    private Label ownedLabel; // "OWNED" label for upgrades already purchased

    // Reusable white texture for backgrounds and boxes
    private TextureRegionDrawable backgroundDrawable;

    // Currently selected upgrade key (null if none selected)
    private String selectedKey = null;

    private Texture background;

    /**
     * Creates the mastery screen with a skill tree grid.
     *
     * @param game         reference to the main game instance
     * @param parentScreen the screen to return to when pressing "Back"
     */
    public MasteryScreen(MazeRunnerGame game, Screen parentScreen) {
        this.game = game;
        this.parentScreen = parentScreen;
        this.manager = MasteryManager.getInstance();
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("mastery_background.png"));

        // Create a 1x1 white pixel texture for backgrounds and boxes
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        backgroundDrawable = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));

        setupUI(); // Build the UI layout
    }

    /**
     * Builds the entire UI layout: title, stats, skill grid, detail panel, and back
     * button.
     * Called initially and whenever the UI needs to be rebuilt (after purchasing an
     * upgrade).
     */
    private void setupUI() {
        stage.clear(); // Remove all old actors

        // Root table that fills the entire screen
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Main container with dark background (pillarboxed to 900px width)
        Table mainContainer = new Table();

        // Add main container with fixed width, but let it grow vertically
        root.add(mainContainer).width(900).growY();

        // --- 1. TITLE ---
        Label title = new Label("MASTERY", game.getSkin(), "title");
        title.setAlignment(Align.center);
        mainContainer.add(title).padTop(40).padBottom(10).expandX().center().row();

        // --- 2. STATS (Skill Points and XP) ---
        pointsLabel = new Label("", game.getSkin());
        pointsLabel.setAlignment(Align.center);
        updateStatsLabel(); // Set the text based on current values
        mainContainer.add(pointsLabel).padBottom(40).expandX().center().row();

        // --- 3. THE SKILL GRID (4 rows, 4 columns each) ---
        Table gridTable = new Table();
        gridTable.defaults().spaceBottom(15); // Space between rows

        // Create a row for each skill category
        createRow(gridTable, "SPEED",
                MasteryManager.SPEED_1, MasteryManager.SPEED_2,
                MasteryManager.SPEED_3, MasteryManager.SPEED_4);

        createRow(gridTable, "DASH",
                MasteryManager.DASH_1, MasteryManager.DASH_2,
                MasteryManager.DASH_3, MasteryManager.DASH_4);

        createRow(gridTable, "ATTACK",
                MasteryManager.ATTACK_1, MasteryManager.ATTACK_2,
                MasteryManager.ATTACK_3, MasteryManager.ATTACK_4);

        createRow(gridTable, "HEALTH",
                MasteryManager.HEALTH_1, MasteryManager.HEALTH_2,
                MasteryManager.HEALTH_3, MasteryManager.HEALTH_4);

        mainContainer.add(gridTable).padBottom(40).expandX().center().row();

        // --- 4. DETAIL PANEL (Shows info about selected upgrade) ---
        Table detailTable = new Table();

        // Upgrade name label
        detailNameLabel = new Label("Select an Upgrade", game.getSkin());
        detailNameLabel.setFontScale(1.2f);
        detailNameLabel.setAlignment(Align.center);
        detailTable.add(detailNameLabel).padTop(10).expandX().center().row();

        // Upgrade description label
        detailDescLabel = new Label("", game.getSkin());
        detailDescLabel.setColor(Color.LIGHT_GRAY);
        detailDescLabel.setAlignment(Align.center);
        detailTable.add(detailDescLabel).pad(10).expandX().center().row();

        // Stack for overlaying purchase button and "OWNED" label
        Stack actionStack = new Stack();

        // Purchase button (only visible if not owned)
        purchaseButton = new TextButton("Purchase", game.getSkin());
        purchaseButton.setVisible(false);
        purchaseButton.getLabel().setAlignment(Align.center);
        purchaseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                handlePurchase();
            }
        });

        // "OWNED" label (only visible if already purchased)
        ownedLabel = new Label("OWNED", game.getSkin());
        ownedLabel.setColor(Color.GREEN);
        ownedLabel.setAlignment(Align.center);
        ownedLabel.setVisible(false);

        // Add both to stack (only one will be visible at a time)
        actionStack.add(purchaseButton);
        actionStack.add(ownedLabel);

        detailTable.add(actionStack).width(320).height(60).pad(10).expandX().center().row();

        mainContainer.add(detailTable).width(600).height(150).expandX().center().row();

        // --- 5. BACK BUTTON ---
        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.getLabel().setAlignment(Align.center);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_back");
                game.setScreen(parentScreen); // Return to previous screen
                dispose();
            }
        });

        mainContainer.add(backBtn).width(200).padTop(50).padBottom(30).expandX().center();

        // Re-select the previously selected upgrade (if any) to refresh the detail
        // panel
        if (selectedKey != null) {
            selectUpgrade(selectedKey);
        }
    }

    /**
     * Creates a single row of the skill grid with a category label and 4 upgrade
     * boxes.
     * Each box represents a tier in the skill tree (1-4).
     *
     * @param parent       the table to add this row to
     * @param categoryName the name to display on the left (SPEED, DASH, etc.)
     * @param k1           upgrade key for tier 1
     * @param k2           upgrade key for tier 2
     * @param k3           upgrade key for tier 3
     * @param k4           upgrade key for tier 4
     */
    private void createRow(Table parent, String categoryName, String k1, String k2, String k3, String k4) {
        // Add category label on the left
        parent.add(new Label(categoryName, game.getSkin())).width(150).align(Align.left);

        // Add 4 upgrade boxes (each tier depends on the previous one)
        addBox(parent, k1, MasteryManager.COST_1, null); // Tier 1 (no prerequisite)
        addBox(parent, k2, MasteryManager.COST_2, k1); // Tier 2 (requires tier 1)
        addBox(parent, k3, MasteryManager.COST_3, k2); // Tier 3 (requires tier 2)
        addBox(parent, k4, MasteryManager.COST_4, k3); // Tier 4 (requires tier 3)

        parent.row(); // Move to next row
    }

    /**
     * Creates a single upgrade box button with appropriate coloring based on
     * ownership status.
     * Color coding:
     * - Green: Already owned
     * - Yellow: Available to purchase (previous tier unlocked)
     * - Gray: Locked (previous tier not unlocked yet)
     *
     * @param parent  the table to add this box to
     * @param key     the upgrade key this box represents
     * @param cost    how many skill points this upgrade costs
     * @param prevKey the prerequisite upgrade key (null for tier 1)
     */
    private void addBox(Table parent, final String key, int cost, String prevKey) {
        final TextButton box = new TextButton("", game.getSkin());

        // Determine box color based on ownership and availability
        boolean isOwned = manager.hasUpgrade(key);
        boolean isPreviousOwned = (prevKey == null) || manager.hasUpgrade(prevKey);

        if (isOwned) {
            box.setColor(Color.GREEN); // Already purchased
        } else if (isPreviousOwned) {
            box.setColor(Color.YELLOW); // Can be purchased
        } else {
            box.setColor(Color.GRAY); // Locked (prerequisite not met)
        }

        // Clicking the box selects it and shows details
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                selectedKey = key;
                selectUpgrade(key);
            }
        });

        parent.add(box).width(50).height(50).padLeft(10).padRight(10);
    }

    /**
     * Updates the detail panel to show information about a specific upgrade.
     * Shows the name, description, and either a purchase button or "OWNED" label.
     *
     * @param key the upgrade key to display details for
     */
    private void selectUpgrade(String key) {
        UpgradeInfo info = getInfo(key);

        // Update labels with upgrade info
        detailNameLabel.setText(info.name);
        detailDescLabel.setText(info.desc);

        // Check ownership and purchase requirements
        boolean isOwned = manager.hasUpgrade(key);
        boolean prevUnlocked = (info.prevKey == null) || manager.hasUpgrade(info.prevKey);
        boolean canAfford = manager.getSkillPoints() >= info.cost;

        // Show either "OWNED" label or purchase button
        ownedLabel.setVisible(isOwned);
        purchaseButton.setVisible(!isOwned);

        if (!isOwned) {
            // Update purchase button text with cost
            purchaseButton.setText("Purchase (" + info.cost + " SP)");

            // Enable button only if prerequisites met and player can afford it
            if (prevUnlocked && canAfford) {
                purchaseButton.setDisabled(false);
                purchaseButton.setColor(Color.WHITE);
            } else {
                purchaseButton.setDisabled(true);
                purchaseButton.setColor(Color.GRAY);
            }
        }
    }

    /**
     * Attempts to purchase the currently selected upgrade.
     * If successful, rebuilds the UI to reflect the new ownership state.
     */
    private void handlePurchase() {
        if (selectedKey == null)
            return;

        UpgradeInfo info = getInfo(selectedKey);

        // Try to unlock the upgrade
        if (manager.unlockUpgrade(selectedKey, info.cost, info.prevKey)) {
            updateStatsLabel(); // Update skill points display
            setupUI(); // Rebuild UI (box colors, button states)
            selectUpgrade(selectedKey); // Refresh detail panel
        }
    }

    /**
     * Updates the stats label to show current skill points and XP progress.
     */
    private void updateStatsLabel() {
        pointsLabel.setText("Skill Points: " + manager.getSkillPoints() +
                "  |  EXP: " + manager.getExperience() + "/100");
    }

    /**
     * Simple container class for upgrade information.
     * Used to avoid repeating the same data retrieval logic.
     */
    private static class UpgradeInfo {
        String name; // Display name of the upgrade
        String desc; // Description of what it does
        String prevKey; // Key of the prerequisite upgrade (null for tier 1)
        int cost; // Skill point cost

        UpgradeInfo(String n, String d, int c, String p) {
            name = n;
            desc = d;
            cost = c;
            prevKey = p;
        }
    }

    /**
     * Gets the display information for a specific upgrade key.
     * Maps upgrade keys to their names, descriptions, costs, and prerequisites.
     *
     * @param key the upgrade key to get info for
     * @return an UpgradeInfo object with all the upgrade's details
     */
    private UpgradeInfo getInfo(String key) {
        switch (key) {
            // SPEED TREE
            case MasteryManager.SPEED_1:
                return new UpgradeInfo("Speed I", "1.25x Movement Speed", 1, null);
            case MasteryManager.SPEED_2:
                return new UpgradeInfo("Speed II", "2.0x Movement Speed", 5, MasteryManager.SPEED_1);
            case MasteryManager.SPEED_3:
                return new UpgradeInfo("Speed III", "2x Stamina Regen", 10, MasteryManager.SPEED_2);
            case MasteryManager.SPEED_4:
                return new UpgradeInfo("Speed IV", "Infinite Stamina", 15, MasteryManager.SPEED_3);

            // DASH TREE
            case MasteryManager.DASH_1:
                return new UpgradeInfo("Dash I", "-25% Cooldown", 1, null);
            case MasteryManager.DASH_2:
                return new UpgradeInfo("Dash II", "-50% Cooldown", 5, MasteryManager.DASH_1);
            case MasteryManager.DASH_3:
                return new UpgradeInfo("Dash III", "-75% Cooldown", 10, MasteryManager.DASH_2);
            case MasteryManager.DASH_4:
                return new UpgradeInfo("Dash IV", "Invincibility during Dash", 15, MasteryManager.DASH_3);

            // ATTACK TREE
            case MasteryManager.ATTACK_1:
                return new UpgradeInfo("Attack I", "Increase Damage by 1", 1, null);
            case MasteryManager.ATTACK_2:
                return new UpgradeInfo("Attack II", "Increase Damage by 2", 5, MasteryManager.ATTACK_1);
            case MasteryManager.ATTACK_3:
                return new UpgradeInfo("Attack III", "Attack Speed +50%", 10, MasteryManager.ATTACK_2);
            case MasteryManager.ATTACK_4:
                return new UpgradeInfo("Attack IV", "One Hit Kill", 15, MasteryManager.ATTACK_3);

            // HEALTH TREE (Life regeneration rates)
            case MasteryManager.HEALTH_1:
                return new UpgradeInfo("Health I", "Regen 1 Life every 40s", 1, null);
            case MasteryManager.HEALTH_2:
                return new UpgradeInfo("Health II", "Regen 1 Life every 20s", 5, MasteryManager.HEALTH_1);
            case MasteryManager.HEALTH_3:
                return new UpgradeInfo("Health III", "Regen 1 Life every 10s", 10, MasteryManager.HEALTH_2);
            case MasteryManager.HEALTH_4:
                return new UpgradeInfo("Health IV", "Regen 1 Life every 5s", 15, MasteryManager.HEALTH_3);

            default:
                return new UpgradeInfo("Unknown", "???", 999, null);
        }
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
     * Renders the mastery screen every frame.
     * Clears to black and draws all UI elements.
     *
     * @param delta time passed since last frame in seconds
     */
    @Override
    public void render(float delta) {
        // Clear background to black
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
     * Called when the window is resized.
     * Updates the stage viewport to match new dimensions.
     *
     * @param width  new window width
     * @param height new window height
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

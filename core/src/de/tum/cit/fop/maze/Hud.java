package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Heads-Up Display (HUD) that shows game information overlaid on top of the gameplay screen.
 * Displays the current level, lives, key status, stamina bar, XP bar, score,
 * and a compass arrow pointing to the current objective.
 *
 * The HUD uses a separate viewport so it stays fixed on screen regardless of camera movement.
 * All UI elements are positioned in screen space rather than world space.
 */
public class Hud {

    //stage used for HUD rendering (currently not using Scene2D actors)
    public Stage stage;

    //viewport used to keep the HUD size consistent across screen sizes.
    private Viewport viewport;

    //font used to render HUD text

    private BitmapFont font;

    //spriteBatch used to draw the HUD elements
    private SpriteBatch batch;

    // A simple 1x1 white pixel texture used to draw colored rectangles (stamina bars, XP bars)
    private Texture blankTexture;

    // compass variables
    private Texture arrowTexture;
    private TextureRegion arrowRegion;

    // List for score
    private ArrayList<ScoreToast> scoreToasts = new ArrayList<>();

    // amount of time you're leveled up for
    private float levelUpTimer = 0f;

    /**
     * Creates a new HUD instance with the specified batch and font.
     * Sets up the viewport, creates textures for bars, and loads the compass arrow.
     *
     * @param batch the shared SpriteBatch used for rendering the HUD
     * @param font  the BitmapFont used to draw all text on the HUD
     */
    public Hud(SpriteBatch batch, BitmapFont font) {
        this.batch = batch;
        this.font = font;

        // create a viewport with a fixed virtual resolution for the HUD
        viewport = new ScreenViewport();


        // create a stage linked to the HUD viewport
        stage = new Stage(viewport, batch);


        // create 1x1 pixel texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        blankTexture = new Texture(pixmap);
        pixmap.dispose();


        // load arrow image
        try {
            arrowTexture = new Texture(Gdx.files.internal("arrow.png"));
            arrowRegion = new TextureRegion(arrowTexture);
        } catch (Exception e) {
            // safety if file is missing, create a red square so the game doesn't crash
            System.err.println("arrow.png not found!");
            Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            p.setColor(Color.RED);
            p.fill();
            arrowTexture = new Texture(p);
            arrowRegion = new TextureRegion(arrowTexture);
            p.dispose();
        }
    }

    /**
     * Adds a floating score popup notification that animates upward and fades out.
     * Used when the player gains or loses points (collecting items, taking damage, etc.).
     *
     * @param text     the text to display (e.g., "+200", "Damage -100")
     * @param positive true for green text (gain), false for red text (loss)
     */
    public void addScorePopup(String text, boolean positive) {
        // Positioned at left margin, Y will be handled by stacking logic in draw()
        scoreToasts.add(new ScoreToast(text, 30, 0, positive));
    }

    /**
     * Renders all HUD elements on the screen.
     * Draws mode, level, lives, key status, stamina bar, XP bar, score, compass, and popups.
     *
     * @param level          current level number
     * @param isStory        true if in story mode, false if in endless mode
     * @param keyCollected   true if the player has collected the key
     * @param lives          player's current number of lives
     * @param status         string describing active status effect (not currently used)
     * @param currentStamina player's current stamina value
     * @param maxStamina     player's maximum stamina capacity
     * @param displayZoom    current camera zoom percentage (50-150%)
     * @param playerX        player's X position in world space
     * @param playerY        player's Y position in world space
     * @param targetX        X position of the current objective (key or exit)
     * @param targetY        Y position of the current objective (key or exit)
     * @param game           reference to main game for accessing textures
     */
    public void draw(int level, boolean isStory, boolean keyCollected, int lives, String status,
                     float currentStamina, float maxStamina, int displayZoom, float playerX, float playerY,
                     float targetX, float targetY, MazeRunnerGame game) {

        // apply the HUD viewport so rendering is done in screen space
        viewport.apply();

        // set projection matrix for HUD rendering
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        // force every color back to solid white so nothing is invisible
        batch.setColor(1, 1, 1, 1);
        font.setColor(1, 1, 1, 1);

        float screenHeight = viewport.getWorldHeight();
        float screenWidth = viewport.getWorldWidth();
        float leftX = 30;
        float topY = screenHeight - 30;

        //  mode
        font.getData().setScale(1.4f);
        String mode = isStory ? "STORY MODE" : "ENDLESS MODE";
        font.draw(batch, mode, leftX, topY);

        // level (increased padding from mode)
        font.getData().setScale(1.0f);
        float levelY = topY - 65;
        font.draw(batch, "Current Level : " + level, leftX, levelY);

        // objective label (increased padding from level)
        float objectiveLabelY = levelY - 55;
        font.setColor(Color.GOLD);
        font.getData().setScale(0.9f);
        font.draw(batch, "Current Objective:", leftX, objectiveLabelY);

        // objective text (line 2)
        String objectiveText = keyCollected ? "FIND THE EXIT" : "FIND THE KEY";
        font.draw(batch, objectiveText, leftX, objectiveLabelY - 25);


        // stamina bars
        float barsMidY = screenHeight / 2 + 30;
        float staminaY = barsMidY;
        float staminaPercent = MathUtils.clamp(currentStamina / maxStamina, 0, 1);

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Stamina", leftX, staminaY + 55);

        batch.setColor(Color.WHITE);
        if (game.getMeterFrame() != null) {
            batch.draw(game.getMeterFrame(), leftX, staminaY, 200, 20);
        }

        if (staminaPercent > 0) {
            float fillX = leftX + 10;
            float fillY = staminaY + 5;
            float fillMaxWidth = 177;
            float fillHeight = 10;

            Texture currentFill = (staminaPercent < 0.25f) ? game.getStaminaLowFill() : game.getStaminaFill();
            if (currentFill != null) {
                batch.draw(currentFill, fillX, fillY, fillMaxWidth * staminaPercent, fillHeight);
            }
        }

        // XP Bar (increased spacing from stamina bar)
        float xpY = staminaY - 85;
        float xpPercent = (float) MasteryManager.getInstance().getExperience() / 100f;

        font.setColor(Color.WHITE);
        font.draw(batch, "XP: " + MasteryManager.getInstance().getExperience() + "%", leftX, xpY + 55);

        batch.setColor(Color.WHITE);
        if (game.getMeterFrame() != null) {
            batch.draw(game.getMeterFrame(), leftX, xpY, 200, 20);
        }

        if (game.getMeterFill() != null && xpPercent > 0) {
            float fillX = leftX + 10;
            float fillY = xpY + 5;
            float fillMaxWidth = 190;
            float fillHeight = 10;
            batch.draw(game.getMeterFill(), fillX, fillY, fillMaxWidth * xpPercent, fillHeight);
        }

        // --- BOTTOM LEFT: SCORE & ZOOM ---
        float zoomY = 40;
        float scoreY = 85;

        font.getData().setScale(1.4f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Zoom: " + displayZoom + "%", leftX, zoomY);

        font.getData().setScale(1.1f);
        font.setColor(Color.GOLD);
        font.draw(batch, "Total Score: " + ScoreManager.getInstance().getLevelTotal(), leftX, scoreY);
        font.getData().setScale(1.0f);

        // --- SCORE TOASTS (STACKED ABOVE SCORE) ---
        int toastIndex = 0;
        Iterator<ScoreToast> it = scoreToasts.iterator();
        while (it.hasNext()) {
            ScoreToast t = it.next();
            t.update(Gdx.graphics.getDeltaTime());

            // Stacking Logic: Base position above score + index offset + internal float animation
            float toastBaseY = scoreY + 40;
            t.x = leftX;
            // Target Y is fixed based on stack position, plus the small upward drift from t.lifeTime
            float drift = 25 * (2.0f - t.lifeTime);
            t.y = toastBaseY + (toastIndex * 30) + drift;

            t.draw(batch, font);

            if (t.lifeTime <= 0) it.remove();
            toastIndex++;
        }


        // lives
        float heartSize = 100f;
        float padding = 10f;
        float totalHeartBlockHeight = (heartSize * 5) + (padding * 4);
        float drawX = screenWidth - heartSize - 80;
        float startY = (screenHeight / 3) - (totalHeartBlockHeight / 2);

        TextureRegion[] hRegions = game.getHealthRegions();
        if (hRegions != null && hRegions.length >= 5) {
            for (int i = 0; i < 5; i++) {
                int livesInThisHeart = MathUtils.clamp(lives - (i * 4), 0, 4);
                int spriteIndex = 4 - livesInThisHeart;
                float drawY = startY + (i * (heartSize + padding));
                batch.draw(hRegions[spriteIndex], drawX, drawY, heartSize, heartSize);
            }
        }

        float compassSize = 80f;
        float compassX = drawX + (heartSize / 2) - (compassSize / 2);
        float compassY = startY + totalHeartBlockHeight + 50;

        font.getData().setScale(1.0f);
        font.setColor(keyCollected ? Color.GREEN : Color.YELLOW);
        String targetLabel = keyCollected ? "EXIT" : "KEY";

        // Centering math for the compass label relative to the hearts/arrow
        float labelX = compassX + (compassSize / 2) - (targetLabel.length() * 5);
        font.draw(batch, targetLabel, labelX, compassY + compassSize + 25);

        float angle = MathUtils.atan2(targetY - playerY, targetX - playerX) * MathUtils.radiansToDegrees;
        batch.setColor(Color.WHITE);
        batch.draw(arrowRegion, compassX, compassY, compassSize / 2, compassSize / 2, compassSize, compassSize, 1f, 1f, angle);

        // level up popup
        if (levelUpTimer > 0) {
            levelUpTimer -= Gdx.graphics.getDeltaTime();
            float alpha = Math.min(1.0f, levelUpTimer);

            font.getData().setScale(1.8f);
            font.setColor(1, 0.8f, 0, alpha);

            String levelUpText = "SKILL POINT EARNED!";
            float textY = 180;
            font.draw(batch, levelUpText, 0, textY, screenWidth, com.badlogic.gdx.utils.Align.center, false);

            font.getData().setScale(1.0f);
        }

        batch.end();
    }

    /**
     *  Process and renders the HUD elements for the Effects from the traps
     *  They show up on the left side of the screen below all the other HUD elements
     *
     * @param player The entity providing the current timer data.
     * @param game The central hub used for asset retrieval.
     */

    public void drawStatusEffects(Player player, MazeRunnerGame game) {
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        float leftX = 30;

        // Positioning below the Experience Bar section (anchored symmetrically)
        float currentY = (viewport.getWorldHeight() / 2) - 110;

        // vine status indicator
        if (player.getRootedTimer() > 0) {
            batch.setColor(Color.GREEN);
            batch.draw(game.getObjectTexture(0, 1), leftX, currentY, 32, 32);
            font.setColor(Color.GREEN);
            font.draw(batch, "ROOTED!", leftX + 45, currentY + 35);
            font.draw(batch, String.format("%.1f", player.getRootedTimer()) + "s", leftX + 45, currentY + 15);
            currentY -= 60;
        }

        // slowness indicator
        if (player.getSlowTimer() > 0) {
            batch.setColor(Color.GRAY);
            batch.draw(game.getObjectTexture(1, 2), leftX, currentY, 32, 32);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "SLOWNESS!", leftX + 45, currentY + 35);
            font.draw(batch, String.format("%.1f", player.getSlowTimer()) + "s", leftX + 45, currentY + 15);
            currentY -= 60;
        }

        // speed boost indicator
        if (player.isSpeedBoostActive()) {
            batch.setColor(Color.YELLOW);
            batch.draw(game.getFastTexture(), leftX, currentY, 32, 32);
            font.setColor(Color.YELLOW);
            font.draw(batch, "SPEED!", leftX + 45, currentY + 45);
            font.draw(batch, String.format("%.1f", player.getSpeedBoostTimer()) + "s", leftX + 45, currentY + 15);
            currentY -= 60;
        }

        // damage boost indicator
        if (player.isDamageBoostActive()) {
            batch.setColor(Color.ORANGE);
            batch.draw(game.getObjectTexture(3, 5), leftX, currentY, 32, 32);
            font.setColor(Color.ORANGE);
            font.draw(batch, "DMG x2!", leftX + 45, currentY + 45);
            font.draw(batch, String.format("%.1f", player.getDamageBoostTimer()) + "s", leftX + 45, currentY + 15);
            currentY -= 60;
        }


        batch.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Triggers the level up notification to display for 3 seconds.
     * Called by GameScreen when the player earns a skill point.
     */
    public void showLevelUpNotification() {
        this.levelUpTimer = 3.0f; // Show for 3 seconds
    }

    /**
     * Called when the window is resized.
     * Updates the HUD viewport to maintain proper scaling and positioning.
     *
     * @param width  the new window width in pixels
     * @param height the new window height in pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Cleans up resources when the HUD is no longer needed.
     * Disposes of textures and the stage to prevent memory leaks.
     */
    public void dispose() {
        if (blankTexture != null) blankTexture.dispose();
        if (arrowTexture != null) arrowTexture.dispose();
        stage.dispose();
    }
}
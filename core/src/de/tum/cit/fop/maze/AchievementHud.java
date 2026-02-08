package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;

/**
 * Handles the visual popup for achievements.
 * Slides in from the left-middle and retreats after 4 seconds.
 */
public class AchievementHud {
    // The achievement currently being displayed as a popup
    private AchievementManager.AchievementEntry currentToast;

    // Counts down from DISPLAY_TIME to 0 while showing the toast
    private float timer = 0;

    // --- TIMING: 0.5s In + 0.75s Stay + 0.5s Out = 1.75s total ---
    private final float DISPLAY_TIME = 1.75f;

    // Current X position of the popup (animates from screen edge inward)
    private float posX = 2000; // Start far off-screen to the right

    /**
     * Updates the achievement popup animation each frame.
     * Pulls the next achievement from the queue when ready.
     *
     * @param delta time passed since last frame in seconds
     */
    public void update(float delta) {
        // If no achievement is currently showing, try to get the next one from the queue
        if (currentToast == null) {
            currentToast = AchievementManager.getInstance().getNextInQueue();
            if (currentToast != null) {
                timer = DISPLAY_TIME;
                // Play achievement sound
                VoiceManager.playAnnouncer("achievement_unlocked");
            }
            return;
        }

        // Count down the display timer
        timer -= delta;

        float screenW = Gdx.graphics.getWidth();
        float targetX = screenW - 500;

        // --- REWRITTEN SMOOTH ANIMATION LOGIC ---

        // PHASE 1: Sliding IN (1.75s down to 1.25s)
        if (timer > 1.25f) {
            float progress = (DISPLAY_TIME - timer) / 0.5f; // 0.0 to 1.0
            posX = Interpolation.pow2Out.apply(screenW, targetX, progress);
        }
        // PHASE 2: STAYING (1.25s down to 0.5s)
        else if (timer > 0.5f) {
            posX = targetX;
        }
        // PHASE 3: Sliding OUT (0.5s down to 0.0s)
        else if (timer > 0) {
            // FIXED: Using pow2Out for a snappier, "one-motion" exit that doesn't feel like it's lagging
            float progress = (0.5f - timer) / 0.5f; // 0.0 to 1.0
            posX = Interpolation.pow2Out.apply(targetX, screenW, progress);
        }

        // When timer runs out, clear the current toast so next one can show
        if (timer <= 0) {
            currentToast = null;
            posX = screenW + 100; // Reset position for safety
        }
    }

    /**
     * Renders the achievement popup text on screen at the current animated position.
     *
     * @param batch the LibGDX batch to draw text to
     * @param font  the font to use for the achievement text
     */
    public void draw(Batch batch, BitmapFont font) {
        // Don't draw anything if no achievement is currently active
        if (currentToast == null) return;

        float screenH = Gdx.graphics.getHeight();
        float startY = screenH - 50;

        // Line 1: Header (Yellow)
        font.getData().setScale(0.85f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "ACHIEVEMENT UNLOCKED!", posX, startY);

        // Line 2: Title (White)
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        font.draw(batch, currentToast.title, posX, startY - 30);

        // Line 3: Description (Grey)
        font.getData().setScale(0.85f);
        font.setColor(Color.LIGHT_GRAY);
        // Wrapping enabled for descriptions
        font.draw(batch, currentToast.desc, posX, startY - 60, 450, com.badlogic.gdx.utils.Align.left, true);

        // Always reset font scale
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
    }
}
package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * A temporary floating text notification that appears when points are gained or lost.
 * The value that shows up depends on the amount of score you gain
 */
public class ScoreToast {
    public String text;

    // Current position in world coordinates
    public float x, y;

    // Remaining time before toast disappears
    public float lifeTime = 1.5f;

    // Color of the text (gold for positive, red for negative)
    private Color color;

    /**
     * The constructor class, which creates a new ScoreToast
     * The color has been set to either gold (positive) or red (negative) for visual feedback.
     *
     * @param text     the score value to display (e.g., "500", "100")
     * @param x        spawn x position in world coordinates
     * @param y        spawn y position in world coordinates
     * @param positive true for bonuses (gold, +), false for penalties (red, -)
     */
    public ScoreToast(String text, float x, float y, boolean positive) {
        this.text = (positive ? "+" : "") + text;
        this.x = x;
        this.y = y;

        // Set color based on score type
        // Gold = reward, Red = penalty
        this.color = positive ? new Color(Color.GOLD) : new Color(Color.RED);
    }

    /**
     * Updates the toast animation every frame.
     * Moves the text upward and counts down the lifetime.
     *
     *
     * @param delta time elapsed since last frame in seconds
     */
    public void update(float delta) {
        lifeTime -= delta;      // Count down to expiration
        y += 25 * delta;        // Float upward (25 pixels per second)
    }

    /**
     * Rendering the score toast with fading out transparency
     *
     * @param batch the SpriteBatch to draw with (must be in drawing state)
     * @param font  the BitmapFont to use (shared with HUD)
     */
    public void draw(Batch batch, BitmapFont font) {
        // calculate transparency
        float alpha = Math.max(0, lifeTime / 2.0f);
        // Apply the color (gold or red) with calculated  transparency
        font.setColor(color.r, color.g, color.b, alpha);

        // Draw the text at current position
        font.draw(batch, text, x, y);

        font.setColor(1, 1, 1, 1);
    }
}

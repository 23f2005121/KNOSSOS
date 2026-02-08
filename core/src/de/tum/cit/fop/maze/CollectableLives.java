package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents a heart item on the ground that gives the player extra lives.
 * When collected, it increases the player's life count and disappears.
 */
public class CollectableLives extends Item {

    // The number of lives this heart restores when picked up
    private final int lifeAmount;

    /**
     * Creates a new heart item at the specified position.
     *
     * @param x             x coordinate in the game world
     * @param y             y coordinate in the game world
     * @param size          width and height of the heart sprite
     * @param textureRegion the image to display for this heart
     * @param lifeAmount    how many lives to give when collected (usually 1)
     */
    public CollectableLives(float x, float y, float size, TextureRegion textureRegion, int lifeAmount) {
        // Initialize the base Item with position and texture
        super(x, y, size, textureRegion);
        this.lifeAmount = lifeAmount;
        // Make sure the heart is visible and collectable from the start
        setActive(true);
    }

    /**
     * Handles what happens when the player touches this heart.
     * Increases player lives, plays a sound, and removes the heart from the map.
     *
     * @param player the player who collected this item
     */
    public void onCollected(Player player) {
        // Don't do anything if this heart was already collected
        if (!isActive())
            return;

        // Play the heart collection sound effect
        VoiceManager.playCollect("collect_heart");

        // Add lives to the player's life count
        player.addLives(lifeAmount);

        // Mark this heart as collected so it disappears and can't be picked up again
        setActive(false);
    }

    /**
     * Draws the heart on screen if it's still active.
     *
     * @param batch the LibGDX batch used for rendering
     */
    @Override
    public void draw(Batch batch) {
        // Only draw if the heart hasn't been collected yet
        if (isActive()) {
            // Use the standard drawing method from the parent GameObject class
            super.draw(batch);
        }
    }
}

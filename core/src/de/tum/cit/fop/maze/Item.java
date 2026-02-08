package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Base class for all collectible and interactable items in the game.
 * This includes keys, hearts (extra lives), power-ups, weapon upgrades, and the exit door.
 * <p>
 * Items have an "active" state that determines whether they're still on the ground.
 * When collected by the player, items are set to inactive so they disappear and can't be picked up again.
 * <p>
 * Extends GameObject to inherit position, size, texture, and collision detection.
 */
public class Item extends GameObject {

    // Whether this item is still on the ground (true) or has been collected (false)
    private boolean active = true;

    /**
     * Creates a new square item at the specified position with the given texture.
     * Items are square (width = height = size) for simplicity.
     * Items start active (visible and collectible) by default.
     *
     * @param x             x position in world coordinates
     * @param y             y position in world coordinates
     * @param size          width and height of the item in pixels
     * @param textureRegion the image to display for this item
     */
    public Item(float x, float y, float size, TextureRegion textureRegion) {
        // Call GameObject constructor with square dimensions
        super(x, y, size, size, textureRegion);
    }

    /**
     * Checks if this item is still on the ground and can be collected.
     * Items that have been picked up return false.
     *
     * @return true if the item is visible and collectible, false if already collected
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether this item is active (on the ground) or inactive (collected).
     * When set to false, the item should no longer be drawn or checked for collision.
     * This is called when the player collects the item.
     *
     * @param active true to make the item visible and collectible, false to hide it
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}

package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents a power-up item that gives the player temporary abilities.
 * Can be Speed (move faster), Shield (block damage), or Damage (deal more damage).
 */
public class CollectablePowerUps extends Item {

    /**
     * Enum defining the different types of power-ups available in the game.
     * Each type triggers a different temporary effect on the player.
     */
    public enum Type {
        SPEED,   // Makes the player move faster for a limited time
        SHIELD,  // Protects the player from damage for a limited time
        DAMAGE   // Increases the player's damage output for a limited time
    }

    // The specific type of this power-up instance (what effect it gives)
    private final Type type;

    // How many seconds the power-up effect lasts after being collected
    private final float duration;

    /**
     * Creates a new power-up item at the specified position.
     *
     * @param x             x coordinate in the game world
     * @param y             y coordinate in the game world
     * @param size          width and height of the power-up sprite
     * @param textureRegion the image to display for this power-up
     * @param type          which type of power-up this is (SPEED, SHIELD, or DAMAGE)
     * @param duration      how long the effect lasts in seconds (e.g., 5.0f)
     */
    public CollectablePowerUps(float x, float y, float size, TextureRegion textureRegion, Type type, float duration) {
        // Initialize the base Item with position and texture
        super(x, y, size, textureRegion);
        this.type = type;
        this.duration = duration;
        // Make the power-up visible and collectable from the start
        setActive(true);
    }

    /**
     * Handles what happens when the player touches this power-up.
     * Activates the appropriate buff on the player based on the type.
     *
     * @param player the player who collected this power-up
     */
    public void onCollected(Player player) {
        // Don't do anything if this power-up was already collected
        if (!isActive())
            return;

        // Play the power-up collection sound effect
        VoiceManager.playCollect("collect_powerup");

        // Activate the correct buff based on what type of power-up this is
        switch (type) {
            case SPEED:
                // Make the player move faster for the specified duration
                player.activateSpeedBoost(duration);
                break;
            case DAMAGE:
                // Increase the player's damage output
                player.activateDamageBoost(duration);
                break;
            default:
                // Do nothing if somehow we have an unknown type
                break;
        }

        // Mark this power-up as collected so it disappears from the map
        setActive(false);
    }

    /**
     * Draws the power-up on screen if it's still active.
     *
     * @param batch the LibGDX batch used for rendering
     */
    @Override
    public void draw(Batch batch) {
        // Only draw if the power-up hasn't been collected yet
        if (isActive()) {
            // Use the standard drawing method from the parent GameObject class
            super.draw(batch);
        }
    }

    /**
     * Gets the type of this power-up.
     *
     * @return the Type enum value (SPEED, SHIELD, or DAMAGE)
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets how long this power-up's effect lasts.
     *
     * @return the duration in seconds
     */
    public float getDuration() {
        return duration;
    }
}

package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents a weapon upgrade collectible that permanently increases player damage.
 * Unlike temporary power-ups (speed boost, shield), weapon upgrades are permanent for the level.
 *
 */
public class Weapons extends GameObject {
    private int damageValue;

    private boolean active = true;

    /**
     * Creates a new weapon collectible at the specified position.
     * Weapons are spawned by MapManager during level initialization.
     *
     * @param x            starting x position in world coordinates (pixels)
     * @param y            starting y position in world coordinates (pixels)
     * @param size         width and height of the weapon sprite (usually 16)
     * @param texture      the visual representation of the weapon
     * @param damageValue  the damage this weapon provides (1-3 typically)
     */
    public Weapons(float x, float y, float size, TextureRegion texture, int damageValue) {
        super(x, y, size, size, texture);
        this.damageValue = damageValue;
    }

    /**
     * Handles the weapon collection logic when the player touches it.
     * Called by GameScreen when player bounds overlap weapon bounds.
     * <p>
     * The weapon upgrade is permanent for the level but resets on:
     * - Level restart (player dies and restarts)
     * - New level (advancing to next level)
     * - Game over (starting new game)
     *
     * @param player the player collecting this weapon
     */
    public void onCollected(Player player) {
        // Only apply collection effect if weapon is still active
        if (active) {
            // Play weapon collection sound for audio feedback
            VoiceManager.playCollect("collect_weapon");

            // Upgrade player's base weapon damage to this weapon's value
            // This replaces the old damage, not adds to it
            // Example: If player has 1 damage, collecting 3 damage weapon → now 3 damage
            player.setWeaponDamage(this.damageValue);

            // Mark weapon as inactive (collected)
            // This prevents re-collection and stops rendering
            active = false;
        }
    }

    /**
     * Checks if the weapon is still available for collection.
     * Used by GameScreen to determine if collision checking is needed.
     * <p>
     * ```
     *
     * @return true if weapon can be collected, false if already collected
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Draws the weapon sprite if it's still active.
     * Inactive weapons (already collected) are not rendered.
     * <p>
     * This creates the visual effect of the weapon disappearing when collected,
     * providing immediate visual feedback to the player.
     *
     * @param batch the SpriteBatch to draw with (must be in drawing state)
     */
    @Override
    public void draw(Batch batch) {
        // Only render if weapon hasn't been collected yet
        if (active)
            super.draw(batch); // Delegate to GameObject.draw()
    }
}

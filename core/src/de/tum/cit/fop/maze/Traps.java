package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents static floor hazards that damage or debuff the player on contact.
 * Traps are stationary obstacles that create danger zones in the level.
 *
 * @author Waleed :>
 */
public class Traps extends Obstacle {
    private int type;

    /**
     * Creates a new trap at the specified position with a given type.
     * Traps are spawned by MapManager during level initialization.
     * <p>
     * The trap type determines the visual appearance and gameplay effect:
     * - Type 0: Spike texture, deals damage
     * - Type 1: Slime texture, applies slowness
     * - Type 2: Vine texture, applies root
     *
     * @param x       starting x position in world coordinates (pixels)
     * @param y       starting y position in world coordinates (pixels)
     * @param width   width of the trap's collision box (usually 16)
     * @param height  height of the trap's collision box (usually 16)
     * @param texture the visual representation of the trap
     * @param type    trap type ID (0=spike, 1=slime, 2=vine)
     */
    public Traps(float x, float y, float width, float height, TextureRegion texture, int type) {
        super(x, y, width, height, texture);
        this.type = type;
    }

    /**
     * Updates the trap logic every frame.
     * Since traps are stationary, this only checks for player contact.
     * <p>
     * Unlike enemies, traps don't need to move or pathfind, making this
     * update method very simple compared to Enemy.update().
     *
     * @param delta      time elapsed since last frame (not used for static traps)
     * @param player     reference to the player for collision detection
     * @param mapManager reference to the map (not used for traps)
     */
    @Override
    public void update(float delta, Player player, MapManager mapManager) {
        // Check if player is standing on/touching this trap
        if (this.contactWithPlayer(player)) {
            // Apply the trap's effect based on type
            applyEffect(player);
        }
    }

    /**
     * Applies the trap's effect to the player based on trap type.
     * The effect depends on the trap type set during construction.
     *
     * @param player the player to apply the effect to
     */
    private void applyEffect(Player player) {
        if (type == 0) {
            // SPIKE TRAP: Deals direct damage
            // No dash immunity - spikes always hurt
            player.takeDamage(1);

        } else if (type == 1) {
            // SLIME TRAP: Applies slowness debuff
            // Dash immunity - skilled players can dash through
            if (!player.isDashing()) {
                player.applySlow(2.0f); // 2 seconds of slowness
            }

        } else if (type == 2) {
            // VINE TRAP: Applies root debuff
            // Dash immunity - skilled players can dash through
            if (!player.isDashing()) {
                player.applyRoot(1.25f); // 1.25 seconds of immobilization
            }
        }
    }

    /**
     * Gets the trap type for identification purposes.
     * Used by MapManager and debugging to determine trap behavior.
     *
     * @return trap type ID (0=spike, 1=slime, 2=vine)
     */
    public int getType() {
        return type;
    }
}

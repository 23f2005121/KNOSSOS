package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Abstract base class for all interactive obstacles in the game.
 * Serves as the parent class for both Traps and Enemy types.
 *
 * @author Waleed :>
 */
public abstract class Obstacle extends GameObject {

    /**
     * Creates a new obstacle with position, size, and texture.
     * Called by subclass constructors to initialize base GameObject properties.
     *
     * @param x       starting x position in world coordinates (pixels)
     * @param y       starting y position in world coordinates (pixels)
     * @param width   width of the obstacle's collision box (pixels)
     * @param height  height of the obstacle's collision box (pixels)
     * @param texture the visual representation of the obstacle
     */
    public Obstacle(float x, float y, float width, float height, TextureRegion texture) {
        super(x, y, width, height, texture);
    }

    /**
     * Updates the obstacle's behavior every frame.
     * This is the brain of each obstacle (specifically for the enemies)
     * where all the pathfinding happens
     *
     * @param delta      time elapsed since last frame in seconds (typically ~0.016s at 60 FPS)
     * @param player     reference to the player for targeting, collision checks, and applying effects
     * @param mapManager reference to the map for pathfinding and wall collision detection
     */
    public abstract void update(float delta, Player player, MapManager mapManager);

    /**
     * Checks if this obstacle is currently touching the player.
     * Returns true if the rectangles of hte player and the obstacle overlap.
     * <p>
     *
     * @param player the player to check collision with
     * @return true if the obstacle's bounds overlap the player's bounds, false otherwise
     */
    public boolean contactWithPlayer(Player player) {
        return this.getBounds().overlaps(player.getBounds());
    }
}

package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents a magic fireball projectile fired by mage enemies.
 * The bolt travels in a straight line toward where the player was when it was fired.
 * It disappears after 3 seconds or when it hits a wall.
 * <p>
 * MageBolts are created by MageEnemy instances and managed by GameScreen.
 * When a bolt hits the player, it deals 1 damage and disappears.
 * <p>
 * Extends GameObject to inherit position, bounds, and basic rendering.
 */
public class MageBolt extends GameObject {

    // Normalized direction vector (values between -1 and 1)
    private final float dirX; // Horizontal direction component
    private final float dirY; // Vertical direction component

    // How fast the bolt moves in pixels per second
    private final float speed = 75f;

    // How long the bolt has been alive
    private float lifeTimer = 0f;

    // Maximum time the bolt can exist before disappearing
    private final float maxLifeTime = 3.0f;

    // Whether the bolt is still flying (false when it hits something or expires)
    private boolean active = true;

    /**
     * Creates a new magic bolt fired from the mage toward the player's current position.
     * The bolt will travel in a straight line in that direction until it hits something or expires.
     *
     * @param x             starting X position (mage's position)
     * @param y             starting Y position (mage's position)
     * @param targetX       X position of the target (player's position when fired)
     * @param targetY       Y position of the target (player's position when fired)
     * @param textureRegion the fireball sprite to display
     */
    public MageBolt(float x, float y, float targetX, float targetY, TextureRegion textureRegion) {
        // Create a small 5x5 hitbox for the fireball
        super(x, y, 5, 5, textureRegion);

        // Calculate the direction vector from mage to player
        float dx = targetX - x;
        float dy = targetY - y;
        // Calculate distance using the Pythagorean theorem
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Normalize the direction vector so speed is consistent regardless of distance
        if (distance != 0) {
            this.dirX = dx / distance; // Range: -1 to 1
            this.dirY = dy / distance; // Range: -1 to 1
        } else {
            // If target is exactly at the mage's position (shouldn't happen), don't move
            this.dirX = 0;
            this.dirY = 0;
        }
    }

    /**
     * Updates the bolt's position and checks for expiration or wall collision.
     * The bolt moves in a straight line each frame based on its direction and speed.
     * It becomes inactive if it hits a wall or exceeds its maximum lifetime.
     *
     * @param delta      time passed since last frame in seconds
     * @param mapManager reference to the map for wall collision checking
     */
    public void update(float delta, MapManager mapManager) {
        if (!active) return; // Don't update if already destroyed

        // Move the bolt in its direction
        x += dirX * speed * delta;
        y += dirY * speed * delta;
        setPosition(x, y); // Sync the collision bounds with the new position

        // Increase the lifetime counter
        lifeTimer += delta;

        // Destroy the bolt if it's been alive too long
        if (lifeTimer > maxLifeTime) {
            active = false;
        }

        // Check if the bolt hit a wall (wait 0.05s to avoid collision with mage's spawn tile)
        if (lifeTimer > 0.05f && mapManager.isCollidingWithWall(getBounds())) {
            active = false;
        }
    }

    /**
     * Draws the fireball sprite if the bolt is still active.
     *
     * @param batch the LibGDX batch used for rendering
     */
    @Override
    public void draw(Batch batch) {
        if (active) {
            // Draw the fireball texture at the current position
            batch.draw(textureRegion, x, y, width, height);
        }
    }

    /**
     * Checks if this bolt is still flying and can hit things.
     *
     * @return true if the bolt is active, false if it hit something or expired
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Marks this bolt as destroyed so it will be removed from the game.
     * Called when the bolt hits the player or needs to be removed.
     */
    public void setInactive() {
        this.active = false;
    }
}

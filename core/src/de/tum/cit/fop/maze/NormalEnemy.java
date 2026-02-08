package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents a standard skeleton enemy in the game.
 * This is the most common enemy type in the game
 * <p>
 * Extends Enemy to inherit pathfinding, collision, and combat logic.
 */
public class NormalEnemy extends Enemy {

    // Reference to main game for accessing skeleton animations
    private final MazeRunnerGame game;

    // Timer that tracks animation frame progression
    private float stateTimer = 0f;

    // Current facing direction (NORTH, SOUTH, EAST, WEST)
    private String currentDirection = "SOUTH";

    // Previous frame's position - used to determine movement direction
    private float lastX, lastY;

    /**
     * Creates a new skeleton enemy at the specified position.
     * Initializes with balanced stats suitable for a standard enemy.
     *
     * @param x    starting x position in world coordinates
     * @param y    starting y position in world coordinates
     * @param game reference to main game for accessing skeleton animations
     */
    public NormalEnemy(float x, float y, MazeRunnerGame game) {
        // Call parent constructor with skeleton-specific stats:
        // Speed: 30f (moderate)
        // Detection: 130f (medium range)
        // Damage: 2 (standard)
        // Health: 3 (average durability)
        super(x, y, 12, 12, null, 30f, 130f, 2, 2);

        this.game = game;
        this.lastX = x;
        this.lastY = y;

        // Death animation duration: 4 frames at 0.15s each = 0.6s total
        this.deathDuration = 0.6f;

        // Try to load initial idle animation facing south
        Animation<TextureRegion> idle = game.getSkeletonAnimation("skeleton_idle", "SOUTH");
        if (idle != null) {
            this.textureRegion = idle.getKeyFrame(0);
        } else {
            // Fallback to old sprite sheet if animations aren't loaded
            this.textureRegion = game.getEnemyTexture(0, 1);
        }
    }

    /**
     * Updates the skeleton's AI, movement, and animations each frame.
     * Runs the parent update for pathfinding and combat, then handles
     * animation selection based on the current state.
     * <p>
     * Animation states:
     * - Death: Playing when isDying is true (non-looping)
     * - Hurt: Brief red flash when recently damaged
     * - Attack: When very close to player (within 20 pixels)
     * - Move: When pathfinding toward player
     * - Idle: When stationary (default)
     *
     * @param delta      time passed since last frame in seconds
     * @param player     reference to the player for AI targeting
     * @param mapManager reference to the map for pathfinding and collision
     */
    @Override
    public void update(float delta, Player player, MapManager mapManager) {
        // 1. Run standard Enemy logic (pathfinding, movement, collision, combat, death timer)
        super.update(delta, player, mapManager);

        // 2. Update global animation timer
        stateTimer += delta;

        // 3. Determine which direction the skeleton is facing based on movement
        // Only calculate direction if alive and not dying
        if (!isDying) {
            // Check if position has changed since last frame
            if (Math.abs(x - lastX) > 0.1f || Math.abs(y - lastY) > 0.1f) {
                // Determine if moving more horizontally or vertically
                if (Math.abs(x - lastX) > Math.abs(y - lastY)) {
                    // Moving horizontally
                    if (x > lastX) currentDirection = "EAST";
                    else currentDirection = "WEST";
                } else {
                    // Moving vertically
                    if (y > lastY) currentDirection = "NORTH";
                    else currentDirection = "SOUTH";
                }
            }
        }

        // Save current position for next frame's direction calculation
        lastX = x;
        lastY = y;

        // 4. Determine which animation state to use
        String stateName = "skeleton_idle"; // Default to idle

        if (isDying) {
            // Playing death animation
            stateName = "skeleton_death";
        } else if (hurtTimer > 0) {
            // Recently hit by player, show hurt animation
            stateName = "skeleton_hurt";
        } else if (currentState == State.ATTACK && isMoving) {
            // Attacking the player
            stateName = "skeleton_move";
            // Calculate distance to player
            float dist = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2));
            // If very close, show attack animation
            if (dist < 20) stateName = "skeleton_attack";
        } else if (isMoving) {
            // Moving but not attacking
            stateName = "skeleton_move";
        }

        // 5. Load and apply the appropriate animation
        Animation<TextureRegion> currentAnim = game.getSkeletonAnimation(stateName, currentDirection);
        if (currentAnim != null) {
            if (isDying) {
                // Death animation plays once without looping
                this.textureRegion = currentAnim.getKeyFrame(deathTimer, false);
            } else {
                // Other animations loop continuously
                this.textureRegion = currentAnim.getKeyFrame(stateTimer, true);
            }
        }
    }
}

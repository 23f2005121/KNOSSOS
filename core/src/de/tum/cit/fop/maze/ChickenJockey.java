package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * The ChickenJockey class represents a fast, flying enemy using bat visuals.
 * Inherits from the Enemy base class and features full directional animations
 * including move, idle, attack, hurt, and death states.
 *
 * */
public class ChickenJockey extends Enemy {

    // Reference to the main game for accessing bat animation assets
    private final MazeRunnerGame game;

    // Timer that tracks animation progression for smooth frame changes
    private float stateTimer = 0f;

    // Stores which way the enemy is facing (NORTH, SOUTH, EAST, WEST)
    private String currentDirection = "SOUTH";

    // Used to detect movement direction by comparing position changes
    private float lastX, lastY;

    /**
     * Creates a new ChickenJockey (bat enemy) at the specified position.
     * This enemy is fast but fragile, designed to harass the player quickly.
     *
     * @param x    starting x position in world coordinates
     * @param y    starting y position in world coordinates
     * @param game reference to the main game for accessing bat animations
     */
    public ChickenJockey(float x, float y, MazeRunnerGame game) {
        // Call parent constructor with ChickenJockey-specific stats:
        // Speed: 40f (very fast, can chase down players)
        // Detection radius: 150f (spots player from far away)
        // Damage: 1 (weak individual hits)
        // Lives: 2 (dies quickly if caught)
        super(x, y, 10, 10, null, 40f, 150f, 1, 2);

        this.game = game;

        // Store initial position for direction tracking
        this.lastX = x;
        this.lastY = y;

        // Death animation has 4 frames at 0.15s each, totaling 0.6 seconds
        this.deathDuration = 0.6f;

        // Try to load the initial idle animation facing south
        Animation<TextureRegion> idle = game.getBatAnimation("bat_idle", "SOUTH");
        if (idle != null) {
            this.textureRegion = idle.getKeyFrame(0);
        } else {
            // Fallback to old sprite sheet if animations aren't loaded
            this.textureRegion = game.getEnemyTexture(4, 2);
        }
    }

    /**
     * Updates the enemy's behavior and animation every frame.
     * Handles pathfinding, movement, collision, direction tracking, and animation selection.
     *
     * @param delta      time passed since last frame in seconds
     * @param player     reference to the player for targeting and collision
     * @param mapManager reference to the map for wall collision checks
     */
    @Override
    public void update(float delta, Player player, MapManager mapManager) {
        // 1. Run the base enemy logic (AI pathfinding, movement, attacks, death timer)
        super.update(delta, player, mapManager);

        // 2. Advance the animation timer for smooth frame transitions
        stateTimer += delta;

        // 3. Figure out which direction the enemy is facing based on movement
        // Only update direction if the enemy is alive and not dying
        if (!isDying) {
            // Check if the enemy has moved since last frame
            if (Math.abs(x - lastX) > 0.1f || Math.abs(y - lastY) > 0.1f) {
                // Compare horizontal vs vertical movement to determine primary direction
                if (Math.abs(x - lastX) > Math.abs(y - lastY)) {
                    // Moving more horizontally
                    if (x > lastX) currentDirection = "EAST";
                    else currentDirection = "WEST";
                } else {
                    // Moving more vertically
                    if (y > lastY) currentDirection = "NORTH";
                    else currentDirection = "SOUTH";
                }
            }
        }

        // Save current position for next frame's direction check
        lastX = x;
        lastY = y;

        // 4. Determine which animation state to display
        String stateName = "bat_idle"; // Default to idle animation

        if (isDying) {
            // Death animation takes priority over everything
            stateName = "bat_death";
        } else if (hurtTimer > 0) {
            // Show hurt animation when recently damaged
            stateName = "bat_hurt";
        } else if (currentState == State.ATTACK && isMoving) {
            // In attack state and moving: show movement animation
            stateName = "bat_move";
            // If very close to player, switch to attack animation
            float dist = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2));
            if (dist < 20) stateName = "bat_attack";
        } else if (isMoving) {
            // Just moving normally
            stateName = "bat_move";
        }

        // 5. Load the correct animation based on state and direction
        Animation<TextureRegion> currentAnim = game.getBatAnimation(stateName, currentDirection);
        if (currentAnim != null) {
            if (isDying) {
                // Death animation plays once and stops at the last frame
                this.textureRegion = currentAnim.getKeyFrame(deathTimer, false);
            } else {
                // All other animations loop continuously
                this.textureRegion = currentAnim.getKeyFrame(stateTimer, true);
            }
        }
    }
}

package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * The Immortal Snail is a special enemy that adds a fun challenge to the game.
 * This enemy is completely invincible and instantly kills the player on contact.
 * It can phase through walls and slowly chases the player once it detects them.
 * <p>
 * Features:
 * - Cannot be killed by the player (ignores all damage)
 * - Moves in a straight line toward the player, ignoring walls
 * - One-hit kills the player on contact
 * - Uses ghost animations with a pulsing translucent effect
 * <p>
 * Stats: Very slow (4f), huge detection range (1500f), instant kill damage (999)
 */
public class ImmortalSnail extends Enemy {

    // Reference to the main game for accessing ghost animation assets
    private final MazeRunnerGame game;

    // How fast the snail moves (intentionally very slow for tension)
    private final float snailSpeed = 4f;

    // How far away the snail can detect the player (covers entire map)
    private final float detectionRadius = 1500f;

    // Once true, the snail will chase the player forever
    private boolean hasDetectedPlayer = false;

    // Animation tracking variables
    private float stateTimer = 0f;              // Tracks animation frame progression
    private String currentDirection = "SOUTH";  // Which way the snail is facing
    private float lastX, lastY;                 // Previous position for direction calculation

    /**
     * Creates a new Immortal Snail at the specified position.
     * The snail starts idle but will begin chasing once it detects the player.
     *
     * @param x    starting x position in world coordinates
     * @param y    starting y position in world coordinates
     * @param game reference to main game for accessing ghost animations
     */
    public ImmortalSnail(float x, float y, MazeRunnerGame game) {
        // Call parent constructor with snail-specific stats:
        // Speed: 4f (very slow chase)
        // Detection: 1500f (can see across entire map)
        // Damage: 999 (instant kill)
        // Health: 9999 (effectively immortal)
        super(x, y, 12, 12, null, 4f, 1500f, 999, 9999);

        this.game = game;
        this.lastX = x;
        this.lastY = y;

        // Death animation duration (not really used since snail can't die)
        this.deathDuration = 0.6f;

        // Try to load the initial ghost idle animation facing south
        Animation<TextureRegion> idle = game.getGhostAnimation("ghost_idle", "SOUTH");
        if (idle != null) {
            this.textureRegion = idle.getKeyFrame(0);
        } else {
            // Fallback texture if animations aren't loaded
            this.textureRegion = game.getEnemyTexture(4, 2);
        }
    }

    /**
     * Override of takeHit that makes the snail invulnerable.
     * Player attacks do nothing except trigger a brief visual "hurt" effect for feedback.
     * The snail's health never decreases.
     *
     * @param damage  the damage amount (ignored)
     * @param sourceX x position of the attack source (ignored)
     * @param sourceY y position of the attack source (ignored)
     */
    @Override
    public void takeHit(int damage, float sourceX, float sourceY) {
        Gdx.app.log("Snail", "Your puny weapon does nothing to the Immortal Snail.");

        // Show the hurt animation briefly so the player knows they hit it
        this.hurtTimer = 0.2f;

        // Intentionally do NOT call super.takeHit() to prevent health reduction
        // The snail cannot die
    }

    /**
     * Updates the snail's movement and animation each frame.
     * Unlike normal enemies, the snail moves in a straight line toward the player
     * and completely ignores walls (phases through them).
     *
     * @param delta      time passed since last frame in seconds
     * @param player     reference to the player for targeting and collision
     * @param mapManager reference to the map (not used for collision since snail phases through walls)
     */
    @Override
    public void update(float delta, Player player, MapManager mapManager) {
        // --- 1. Update timers and animation ---
        // Handle the hurt effect timer manually since we're not calling super.update()
        if (hurtTimer > 0) hurtTimer -= delta;
        stateTimer += delta;

        // --- 2. Movement Logic ---
        // Calculate the direction vector from snail to player
        float dx = player.getX() - x;
        float dy = player.getY() - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Once the player enters detection range, the snail locks on forever
        if (distance < detectionRadius) {
            hasDetectedPlayer = true;
        }

        isMoving = false;

        // If the snail has detected the player, move directly toward them
        if (hasDetectedPlayer && distance > 0) {
            isMoving = true;

            // Move in a straight line toward the player, ignoring all walls
            // This is what makes the snail "phase" through obstacles
            x += (dx / distance) * snailSpeed * delta;
            y += (dy / distance) * snailSpeed * delta;

            // Keep the hitbox in sync with the visual position
            setPosition(x, y);

            // Check if the snail touched the player -> instant death
            if (contactWithPlayer(player)) {
                player.takeDamage(999); // One-shot kill
            }
        }

        // --- 3. Determine which direction the snail is facing ---
        // Compare current position to last frame's position
        if (Math.abs(x - lastX) > 0.01f || Math.abs(y - lastY) > 0.01f) {
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

        // Save current position for next frame's direction calculation
        lastX = x;
        lastY = y;

        // --- 4. Decide which animation to show ---
        String stateName = "ghost_idle"; // Default to idle

        if (hurtTimer > 0) {
            // Show hurt animation when player attacks
            stateName = "ghost_hurt";
        } else if (hasDetectedPlayer && isMoving) {
            // Moving toward player
            stateName = "ghost_move";
            // Show attack animation when very close to player
            if (distance < 20) stateName = "ghost_attack";
        } else if (isMoving) {
            // Just moving normally
            stateName = "ghost_move";
        }

        // --- 5. Load and apply the correct animation ---
        Animation<TextureRegion> currentAnim = game.getGhostAnimation(stateName, currentDirection);
        if (currentAnim != null) {
            this.textureRegion = currentAnim.getKeyFrame(stateTimer, true);
        }
    }

    /**
     * Draws the snail with a ghostly pulsing effect.
     * The snail has a translucent cyan tint that pulses to give it an ethereal appearance.
     * Turns red briefly when hit by the player.
     *
     * @param batch the LibGDX batch used for rendering
     */
    @Override
    public void draw(Batch batch) {
        // Create a pulsing alpha effect using the frame counter
        float pulse = (float) Math.sin(Gdx.graphics.getFrameId() * 0.05f) * 0.2f;

        // Choose color: red if recently hit, otherwise ghostly cyan
        if (hurtTimer > 0) {
            batch.setColor(1f, 0.3f, 0.3f, 1f); // Red tint when hurt
        } else {
            // Translucent cyan with pulsing alpha (0.6 to 1.0)
            batch.setColor(0.62f, 0.84f, 1f, 0.8f + pulse);
        }

        // Draw the snail sprite with the applied color tint
        super.draw(batch);

        // Reset color to white so other objects aren't tinted
        batch.setColor(Color.WHITE);
    }
}

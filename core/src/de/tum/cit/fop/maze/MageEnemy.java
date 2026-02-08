package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * A stationary ranged enemy that fires magic bolts at the player.
 * The mage doesn't move but compensates by having long-range attacks.
 * <p>
 * Attack pattern:
 * 1. Detects player within 250 pixel range
 * 2. Checks if there's line of sight (no walls blocking)
 * 3. Charges for 1 second (plays attack animation)
 * 4. Fires a magic bolt (GameScreen spawns the projectile)
 * 5. Cooldown for 2 seconds before next shot
 * <p>
 * Stats: 0 speed (stationary), 250f detection range, 1 damage, 2 health
 * Uses 'mage_*.png' animations for idle, hurt, attack, and death states.
 */
public class MageEnemy extends Enemy {

    // Cooldown timer that counts down between shots
    private float shootTimer = 0f;

    // How long the mage must wait between firing shots
    private final float COOLDOWN_TIME = 2.0f;

    // How long the mage charges up before releasing a shot
    private final float CHARGE_TIME = 1.0f;

    // Whether the mage is currently charging up a shot
    private boolean isCharging = false;

    // Flag that signals GameScreen to spawn a MageBolt projectile
    private boolean shotReady = false;

    // Reference to main game for accessing mage animations
    private final MazeRunnerGame game;

    // Animation state tracking
    private float stateTimer = 0f;          // Tracks animation frame progression
    private String currentDirection = "SOUTH"; // Which way the mage is facing

    /**
     * Creates a new stationary mage enemy at the specified position.
     * The mage will automatically track and shoot at the player when in range.
     *
     * @param x    x position in world coordinates
     * @param y    y position in world coordinates
     * @param game reference to main game for accessing mage animation assets
     */
    public MageEnemy(float x, float y, MazeRunnerGame game) {
        // Call parent with mage-specific stats:
        // Speed: 0 (completely stationary)
        // Detection: 250f (long range sniper)
        // Damage: 1 (from bolt projectile)
        // Health: 2 (fragile)
        super(x, y, 16, 16, null, 0, 250f, 1, 2);
        this.game = game;

        // Death animation duration
        this.deathDuration = 0.6f;

        // Load initial idle animation facing south
        Animation<TextureRegion> idle = game.getMageAnimation("mage_idle", "SOUTH");
        if (idle != null) {
            this.textureRegion = idle.getKeyFrame(0);
        } else {
            // Fallback to old sprite sheet if animations aren't loaded
            this.textureRegion = game.getEnemyTexture(0, 3);
        }
    }

    /**
     * Updates the mage's combat logic and animations.
     * Completely overrides the parent update to avoid movement/pathfinding logic
     * since the mage is stationary.
     *
     * Handles:
     * - Death animation timing
     * - Hurt effect timing
     * - Direction calculation (mage faces the player)
     * - Charging and firing logic
     * - Contact damage if player touches the mage
     *
     * @param delta      time passed since last frame in seconds
     * @param player     reference to the player for targeting
     * @param mapManager reference to the map for line of sight checks
     */
    @Override
    public void update(float delta, Player player, MapManager mapManager) {
        // --- 1. HANDLE DEATH & HIT TIMERS ---
        // We don't call super.update() to avoid movement logic, so we handle death manually
        if (isDead())
            return;

        // Handle death animation
        if (isDying) {
            deathTimer += delta;
            if (deathTimer >= deathDuration) {
                // Death animation finished, entity can be removed by GameScreen
            } else {
                // Still playing death animation
                updateAnimation(delta);
                return; // Skip combat logic while dying
            }
        }

        // Trigger actual removal after animation completes
        if (isDying && deathTimer >= deathDuration) {
            super.update(delta, player, mapManager); // Sets isDead = true in parent
            return;
        }

        // Count down the hurt timer (red tint when hit)
        if (hurtTimer > 0)
            hurtTimer -= delta;

        // Update animation frame counter
        stateTimer += delta;

        // --- 2. CALCULATE DIRECTION (Mage is stationary but looks at player) ---
        float dx = player.getX() - x;
        float dy = player.getY() - y;

        // Determine which direction to face based on player position
        if (Math.abs(dx) > Math.abs(dy)) {
            currentDirection = (dx > 0) ? "EAST" : "WEST";
        } else {
            currentDirection = (dy > 0) ? "NORTH" : "SOUTH";
        }

        // --- 3. COMBAT LOGIC ---
        // Count down the reload timer between shots
        if (shootTimer > 0)
            shootTimer -= delta;

        // Calculate distance to player
        float dist = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2));

        // Check if player is in range and there's a clear line of sight
        if (dist < 250f && shootTimer <= 0) {
            if (mapManager.hasLineOfSight(x + 8, y + 8, player.getX() + 4, player.getY() + 4)) {
                isCharging = true; // Start charging up a shot
            } else {
                isCharging = false; // Lost line of sight, cancel charge
            }
        }

        // Handle charging state
        if (isCharging) {
            // Use shootTimer as negative counter during charge phase
            shootTimer -= delta;
            if (shootTimer <= -CHARGE_TIME) {
                fireBolt(); // Charge complete, fire!
            }
        }

        // Deal damage if player touches the mage directly
        if (contactWithPlayer(player)) {
            player.takeDamage(1);
        }

        updateAnimation(delta);
    }

    /**
     * Updates the mage's animation based on its current state.
     * Chooses the appropriate animation (idle, hurt, attack, death) and updates the texture.
     *
     * @param delta time passed since last frame
     */
    private void updateAnimation(float delta) {
        String stateName = "mage_idle"; // Default to idle

        // Choose animation based on current state
        if (isDying) {
            stateName = "mage_death";
        } else if (hurtTimer > 0) {
            stateName = "mage_hurt";
        } else if (isCharging) {
            stateName = "mage_attack"; // Show attack animation while charging
        }

        // Load the appropriate animation
        Animation<TextureRegion> currentAnim = game.getMageAnimation(stateName, currentDirection);
        if (currentAnim != null) {
            if (isDying) {
                // Death animation plays once (not looping)
                this.textureRegion = currentAnim.getKeyFrame(deathTimer, false);
            } else {
                // Other animations loop
                this.textureRegion = currentAnim.getKeyFrame(stateTimer, true);
            }
        }
    }

    /**
     * Fires a magic bolt at the player.
     * Sets the shotReady flag so GameScreen knows to spawn a MageBolt projectile.
     * Also resets the cooldown timer and plays the shooting sound effect.
     */
    private void fireBolt() {
        this.shotReady = true;          // Signal GameScreen to create a bolt
        this.isCharging = false;        // Stop charging
        this.shootTimer = COOLDOWN_TIME; // Start cooldown before next shot
        VoiceManager.playEnemy("enemy_mage_shoot"); // Play shooting sound
    }

    /**
     * Draws the mage with special visual effects.
     * Shows a red tint when recently hit by the player.
     *
     * @param batch the LibGDX batch used for rendering
     */
    @Override
    public void draw(Batch batch) {
        if (isDead() && !isDying)
            return; // Completely dead, don't draw

        // Apply red tint when hurt for visual feedback
        if (hurtTimer > 0) {
            batch.setColor(1, 0.3f, 0.3f, 1);
        }

        // Draw the current animation frame
        super.draw(batch);

        // Reset color so other objects aren't tinted
        batch.setColor(Color.WHITE);
    }

    /**
     * Checks if the mage has a shot ready to fire.
     * This is called by GameScreen to know when to spawn a MageBolt projectile.
     * Automatically resets the flag after being read.
     *
     * @return true if a bolt should be spawned this frame, false otherwise
     */
    public boolean hasShotReady() {
        if (shotReady) {
            shotReady = false; // Reset the trigger
            return true;
        }
        return false;
    }

    /**
     * Checks if the mage is currently charging up a shot.
     * Can be used for visual effects or UI indicators.
     *
     * @return true if the mage is charging, false otherwise
     */
    public boolean isCharging() {
        return isCharging;
    }
}

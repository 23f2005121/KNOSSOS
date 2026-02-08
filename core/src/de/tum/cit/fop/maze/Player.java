package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;

/**
 * The main character controlled by the user.
 * Handles movement, collisions, lives, and power-up effects.
 */
public class Player extends GameObject {
    private float stateTime = 0f; // Track time for animation frames
    private final ArrayList<Integer> inputStack; // Stores keys pressed to handle smooth movement

    private enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    private Direction currentDirection = Direction.SOUTH; // Default

    // Combat variables
    private int weaponDamage = 1; // Start with basic sword (1 damage)
    private float attackTimer = 0f; // Tracks how long the swing lasts
    private float attackCooldown = 0f; // Prevents the player from spamming spacer
    private final float ATTACK_DURATION = 0.2f; // Sword is "out" for 0.2 seconds
    private final float ATTACK_COOLDOWN_TIME = 0.3f; // Wait 0.3s between swings

    // Stats
    private int lives = 20; // Current number of lives. Game Over if this hits 0.
    private float walkSpeed = 50f; // Base walking speed of the player.
    private final float dashSpeed = 400f;

    // Status Effects
    private boolean isDashing = false;
    private float dashTimer = 0f;
    private float dashCooldownTimer = 0f;

    // PowerUps
    // Timer for the speed boost. If > 0, player moves faster.
    private float speedBoostTimer = 0f;
    // Timer for our 2x Damage
    private float damageBoostTimer = 0f;

    // Debuffs
    private float rootedTimer = 0f; // Stops movement
    private float slowTimer = 0f; // Reduces speed
    private float invincibilityTimer = 0f; // Grace period after hit

    // Direction
    private float facingX = 0f;
    private float facingY = -1f;

    // Stamina variables
    private float maxStamina = 100f;
    private float currentStamina = 100f;
    private boolean isSprintPressed = false;

    // How fast stamina depletes when sprinting (50 units/second)
    private float staminaDepletionRate = 50f;

    // How fast stamina regenerates when not sprinting (10 units/second)
    private float staminaRegenRate = 10f;

    // Exhaustion state: Cannot sprint again until stamina recovers to 20+
    private boolean exhausted = false;

    // cheats
    private boolean ghostMode = false;
    private boolean permanentImmortal = false;

    // mastery
    private final MasteryManager mastery = MasteryManager.getInstance();
    private float healthRegenTimer = 0f;

    private float timeSinceDeath = 0f;

    // Sound timers
    private float footstepTimer = 0f;
    private float breathingTimer = 0f;
    private final float footstepInterval = 0.35f;
    private final float breathingInterval = 1.2f;

    /**
     * Creates a new player at the specified position.
     * Initializes all systems to their default states.
     * The player starts facing south with full health and stamina.
     *
     * @param x      starting x position in world coordinates
     * @param y      starting y position in world coordinates
     * @param width  width of the player sprite (16 pixels)
     * @param height height of the player sprite (16 pixels)
     */

    public Player(float x, float y, float width, float height) {
        super(x, y, width, height, null); // Pass null for initial texture, we calculate it in draw
        this.inputStack = new ArrayList<>();
        // Requirement: Player size 12 and bounds 12
        this.bounds.setSize(12f, 12f);
    }

    // Input

    /**
     * Called when a key is pressed. Adds the key to a stack to handle smooth
     * movement changes.
     */
    public void keyDown(int keycode, GamePreferences prefs) {
        if (rootedTimer > 0)
            return; // Cannot move if rooted

        if (keycode == prefs.getKeyAttack()) {
            this.attemptAttack();
            return;
        }

        if (keycode == prefs.getKeyDash()) {
            attemptDash();
            return; // Dash
        }

        if (keycode == prefs.getKeySprint()) {
            isSprintPressed = true;
        } // sprint

        if (!inputStack.contains(keycode)) {
            if (keycode == prefs.getKeyUp() || keycode == prefs.getKeyDown() || keycode == prefs.getKeyLeft()
                    || keycode == prefs.getKeyRight()) {
                inputStack.add(keycode);
            }
        }

    }

    /**
     * Called when a key is released.
     * Removes the key from the input stack, allowing the previous direction to resume.
     * Also handles sprint key release to stop sprinting.
     *
     * @param keycode the LibGDX keycode that was released
     * @param prefs   user preferences for key bindings
     */
    public void keyUp(int keycode, GamePreferences prefs) {
        if (keycode == prefs.getKeySprint()) {
            isSprintPressed = false;
        }
        inputStack.remove(Integer.valueOf(keycode));
    }

    /**
     * Resets all input and temporary status effects.
     * Called when restarting a level, pausing, or entering a new area.
     * Ensures the player doesn't start with lingering effects or stuck inputs.
     */
    public void clearInput() {
        inputStack.clear();
        isDashing = false;
        isSprintPressed = false; // Added to prevent stuck sprinting on pause
        speedBoostTimer = 0;
        rootedTimer = 0;
        slowTimer = 0;
        invincibilityTimer = 0;
        damageBoostTimer = 0;
    }

    // Game Logic

    /**
     * Updates the player logic every frame.(60 times per second [60 FPS])
     *
     * @param delta      Time passed since last frame (in seconds).
     * @param mapManager Reference to the map for collision checking.
     * @param prefs      User key bindings.
     */

    public void update(float delta, MapManager mapManager, GamePreferences prefs, MazeRunnerGame game) {
        stateTime += delta;

        // death logic
        if (lives <= 0) {
            timeSinceDeath += delta;

            // force the death animation
            Animation<TextureRegion> deathAnim = game.getPlayerAnimation("death", currentDirection.toString());
            if (deathAnim != null) {


                // use timeSinceDeath instead of stateTime so it starts from frame 0
                this.textureRegion = deathAnim.getKeyFrame(timeSinceDeath, false);
            }

            // sync position and Stop here (no movement, no regen)
            this.setPosition(x, y);
            return;
        }

        // --- 2. TIMERS ---
        if (attackTimer > 0)
            attackTimer -= delta;
        if (attackCooldown > 0)
            attackCooldown -= delta;
        if (dashCooldownTimer > 0)
            dashCooldownTimer -= delta;
        if (speedBoostTimer > 0)
            speedBoostTimer -= delta;
        if (invincibilityTimer > 0)
            invincibilityTimer -= delta;
        if (damageBoostTimer > 0)
            damageBoostTimer -= delta;

        // Slow timer with grace period on expiration
        if (slowTimer > 0) {
            slowTimer -= delta;
            if (slowTimer <= 0) {
                slowTimer = 0;
                // Grant brief invincibility when slow effect ends
                this.invincibilityTimer = 0.5f;
            }
        }

        // logic for the vine traps
        if (rootedTimer > 0) {
            rootedTimer -= delta;

            // grants a grace period so that the player can safely escape the vine
            if (rootedTimer <= 0) {
                rootedTimer = 0;
                this.invincibilityTimer = 1.0f; // escape window
                Gdx.app.log("Player", "Free from vines! Escape window active.");
            }

            this.setPosition(x, y);
            // show idle animation while rooted
            if (textureRegion == null) {
                this.textureRegion = game.getPlayerAnimation("idle", currentDirection.toString()).getKeyFrame(stateTime,
                        true);
            }
            return; // just return, cause player can't move
        }

        // health regen logic
        float regenInterval = -1f;
        if (mastery.hasUpgrade(MasteryManager.HEALTH_4))
            regenInterval = 5f;
        else if (mastery.hasUpgrade(MasteryManager.HEALTH_3))
            regenInterval = 10f;
        else if (mastery.hasUpgrade(MasteryManager.HEALTH_2))
            regenInterval = 20f;
        else if (mastery.hasUpgrade(MasteryManager.HEALTH_1))
            regenInterval = 40f;

        if (regenInterval > 0) {
            healthRegenTimer += delta;
            if (healthRegenTimer >= regenInterval) {
                addLives(1);
                healthRegenTimer = 0;
            }
        }

        // stamina logic
        boolean hasInput = !inputStack.isEmpty();
        boolean actuallySprinting = false;
        boolean unlimitedStamina = mastery.hasUpgrade(MasteryManager.SPEED_4);
        float currentRegenRate = mastery.hasUpgrade(MasteryManager.SPEED_3) ? (staminaRegenRate * 2) : staminaRegenRate;

        if (isSprintPressed && hasInput && !exhausted && !isDashing) {
            if (!unlimitedStamina) {
                currentStamina -= staminaDepletionRate * delta;
                if (currentStamina <= 0) {
                    currentStamina = 0;
                    exhausted = true;
                }
            }
            actuallySprinting = true;
        } else {
            if (currentStamina < maxStamina) {
                currentStamina += currentRegenRate * delta;
                if (currentStamina > maxStamina)
                    currentStamina = maxStamina;
            }
            if (exhausted && currentStamina > 20)
                exhausted = false;
        }

        // movement logic

        // recording positions before we moved
        float startX = x;
        float startY = y;

        if (isDashing) {
            updateDash(delta, mapManager);
        } else {
            updateWalking(delta, mapManager, prefs, actuallySprinting);
        }

        // determine if we actually moved
        boolean isActuallyMoving = (Math.abs(x - startX) > 0.1f || Math.abs(y - startY) > 0.1f);

        // checking if the player actually moves, then playing the footstep sound
        if (isActuallyMoving) {
            footstepTimer += delta;
            float currentInterval = actuallySprinting ? (footstepInterval * 0.6f) : footstepInterval;

            if (footstepTimer >= currentInterval) {
                VoiceManager.playRandom(VoiceManager.Category.PLAYER, "player_step_1", "player_step_2");
                footstepTimer = 0f;
            }
        } else {
            footstepTimer = 0f; // Reset if blocked by wall
        }

        // --- SPRINT BREATHING SOUND ---
        if (actuallySprinting) {
            breathingTimer += delta;
            if (breathingTimer >= breathingInterval) {
                VoiceManager.playPlayer("player_sprint");
                breathingTimer = 0f;
            }
        } else {
            breathingTimer = 0f;
        }


        // animation logic
        String stateName = "idle";
        boolean looping = true;
        float animTime = stateTime;

        if (invincibilityTimer > 0.5f) {
            stateName = "hurt";
        } else if (isAttacking()) {
            stateName = "attack";
            looping = false;
            animTime = ATTACK_DURATION - attackTimer;
        } else if (isDashing || (hasInput && isActuallyMoving)) { // if either dashing or moving, play the move animation
            stateName = "move";
        }

        Animation<TextureRegion> anim = game.getPlayerAnimation(stateName, currentDirection.toString());
        if (anim != null) {
            this.textureRegion = anim.getKeyFrame(animTime, looping);
        }

        this.setPosition(x, y);
    }

    /**
     * Attempts to perform a dash if not on cooldown.
     * Dash is a short (0.15s) burst of high-speed movement.
     * Cooldown is affected by mastery upgrades (0.25x to 1.0x).
     *
     */
    private void attemptDash() {
        if (!isDashing && dashCooldownTimer <= 0 && rootedTimer <= 0) {
            isDashing = true;
            dashTimer = 0.15f;

            VoiceManager.playPlayer("player_dash"); // Sound effect

            // Mastery Cooldown Reduction
            float baseCooldown = 1.0f;
            if (mastery.hasUpgrade(MasteryManager.DASH_3))
                baseCooldown *= 0.25f;
            else if (mastery.hasUpgrade(MasteryManager.DASH_2))
                baseCooldown *= 0.50f;
            else if (mastery.hasUpgrade(MasteryManager.DASH_1))
                baseCooldown *= 0.75f;

            dashCooldownTimer = baseCooldown;
        }
    }

    /**
     * Updates dash movement.
     * Moves at high speed in the facing direction, checking for wall collisions.
     * Ends dash when timer expires.
     *
     * @param delta      time since last frame
     * @param mapManager map reference for collision detection
     */
    private void updateDash(float delta, MapManager mapManager) {
        float moveAmount = dashSpeed * delta;
        applyMovement(moveAmount, facingX, facingY, mapManager);
        dashTimer -= delta;
        if (dashTimer <= 0)
            isDashing = false;
    }

    /**
     * Initiates a melee attack if cooldown has expired.
     * Creates a temporary hitbox in front of the player for 0.2 seconds.
     * <p>
     * Cannot attack if rooted (vine trap).
     */
    public void attemptAttack() {
        // Only attack if not on cooldown and not rooted (vines)
        if (attackCooldown <= 0 && rootedTimer <= 0) {
            attackTimer = ATTACK_DURATION;

            // if attack3, reduce cooldown for attack
            float finalCooldown = ATTACK_COOLDOWN_TIME;
            if (mastery.hasUpgrade(MasteryManager.ATTACK_3)) {
                finalCooldown *= 0.5f; // reducing wait time by 0.5s
            }

            attackCooldown = finalCooldown;
            VoiceManager.playPlayer("player_attack_1");
            Gdx.app.log("Player", "Swing! Damage: " + getWeaponDamage());
        }
    }

    /**
     * Handles walking movement with collision detection.
     * Applies speed multipliers from buffs, debuffs, and mastery upgrades.
     * Plays footstep and breathing sounds based on movement state.
     *
     *
     * @param delta        time since last frame
     * @param mapManager   map reference for collision detection
     * @param prefs        user key bindings
     * @param isSprinting  whether sprint is currently active
     */

    private void updateWalking(float delta, MapManager mapManager, GamePreferences prefs, boolean isSprinting) {
        if (inputStack.isEmpty())
            return;

        // Calculate Speed Multiplier
        float speedMult = 1.0f;

        // Mastery Speed Upgrades
        if (mastery.hasUpgrade(MasteryManager.SPEED_2))
            speedMult *= 2.0f;
        else if (mastery.hasUpgrade(MasteryManager.SPEED_1))
            speedMult *= 1.25f;

        if (speedBoostTimer > 0)
            speedMult *= 1.5f; // If speed boost is active, increase multiplier by 50%
        if (isSprinting)
            speedMult *= 1.5f; // Increase speed by 75%
        if (slowTimer > 0)
            speedMult *= 0.4f; // If slow trap is active decrease speed by 60%

        // Limiting speed to prevent wall phasing
        speedMult = Math.min(speedMult, 2.2f);

        float currentSpeed = walkSpeed * speedMult;
        float moveAmount = currentSpeed * delta;

        int currentKey = inputStack.get(inputStack.size() - 1); // Get the most recent key pressed
        float dx = 0, dy = 0;

        if (currentKey == prefs.getKeyLeft()) {
            dx = -1;
            facingX = -1;
            facingY = 0;
            currentDirection = Direction.WEST;
        } else if (currentKey == prefs.getKeyRight()) {
            dx = 1;
            facingX = 1;
            facingY = 0;
            currentDirection = Direction.EAST;
        } else if (currentKey == prefs.getKeyUp()) {
            dy = 1;
            facingX = 0;
            facingY = 1;
            currentDirection = Direction.NORTH;
        } else if (currentKey == prefs.getKeyDown()) {
            dy = -1;
            facingX = 0;
            facingY = -1;
            currentDirection = Direction.SOUTH;
        }

        applyMovement(moveAmount, dx, dy, mapManager);
    }

    /**
     * Applies movement in the specified direction with wall collision detection.
     * Uses axis-aligned collision (X and Y checked separately) for smooth wall sliding.
     * Ghost mode bypasses all collision detection.
     *
     * @param moveAmount distance to move in pixels
     * @param dx         horizontal direction (-1, 0, or 1)
     * @param dy         vertical direction (-1, 0, or 1)
     * @param mapManager map reference for wall collision checks
     */

    private void applyMovement(float moveAmount, float dx, float dy, MapManager mapManager) {
        if (ghostMode) {
            x += dx * moveAmount;
            y += dy * moveAmount;
            setPosition(x, y);
            return;
        }

        // --- Horizontal Movement with Snapping ---
        float stepX = dx * moveAmount;
        if (Math.abs(stepX) > 0) {
            float originalX = x;
            x += stepX;
            bounds.setPosition(x, y);
            if (mapManager.isCollidingWithWall(bounds)) {
                x = originalX;
                // High-precision Snapping: Try to get as close as possible in 0.1px increments
                float snapStep = 0.1f * Math.signum(stepX);
                while (Math.abs(x - (originalX + stepX)) > 0.05f) {
                    bounds.setPosition(x + snapStep, y);
                    if (mapManager.isCollidingWithWall(bounds)) break;
                    x += snapStep;
                }
            }
        }

        // --- Vertical Movement with Snapping ---
        float stepY = dy * moveAmount;
        if (Math.abs(stepY) > 0) {
            float originalY = y;
            y += stepY;
            bounds.setPosition(x, y);
            if (mapManager.isCollidingWithWall(bounds)) {
                y = originalY;
                // High-precision Snapping: Try to get as close as possible in 0.1px increments
                float snapStep = 0.1f * Math.signum(stepY);
                while (Math.abs(y - (originalY + stepY)) > 0.05f) {
                    bounds.setPosition(x, y + snapStep);
                    if (mapManager.isCollidingWithWall(bounds)) break;
                    y += snapStep;
                }
            }
        }

        setPosition(x, y);
    }

    /**
     * Called when the player hits a trap or enemy.
     * Handles removing lives and invincibility checks.
     *
     * @param amount The damage to take.
     */

    public void takeDamage(int amount) {
        if (permanentImmortal || invincibilityTimer > 0)
            return;

        // Play hurt sound
        VoiceManager.playRandom(VoiceManager.Category.PLAYER, "player_hurt_1", "player_hurt_2");

        if (isDashing && mastery.hasUpgrade(MasteryManager.DASH_4)) {
            // if you're dashing and the player has the dash-4 mastery
            return; // just return, you take no damage
        }

        // first life lost, thus trigger this achievement
        AchievementManager.getInstance().unlock("FIRST_DEATH");

        // check if you died very early
        if (stateTime < 5.0f)
            AchievementManager.getInstance().unlock("EARLY_DEATH");

        this.lives -= amount;
        this.invincibilityTimer = 1.0f;
    }

    /**
     * Generates a temporary Rectangle representing the sword's reach.
     * Uses the current 'facing' direction to project a hitbox
     * in front of the player. And from that hitbox we calculate
     * If we hit an enemy or not
     *
     * @return A Rectangle for collision detection, or null if not attacking.
     */
    public com.badlogic.gdx.math.Rectangle getAttackHitbox() {
        if (!isAttacking())
            return null;

        /*
         * our hitbox variables, basically the way the attack works in general is that
         * we
         * project a hitbox in front of the player, and if our enemy comes in contact
         * with this
         * hitbox, the enemy takes damage/dies from the player
         */

        float hitboxSize = 8f;
        /*
         * size of the hitbox, our character is 16px, so always remember that, cause the
         * tip of our weapon/reach is 16 + the hitbox size
         */
        float hX = x;
        float hY = y;

        // first we check
        if (facingX != 0) {
            if (facingX > 0)
                hX = x + width;
            else
                hX = x - hitboxSize;
        } else if (facingY != 0) {
            if (facingY > 0)
                hY = y + height;
            else
                hY = y - hitboxSize;
        }

        return new com.badlogic.gdx.math.Rectangle(hX, hY, hitboxSize, hitboxSize);
    }

    // Status Effect Applicators (Used by Traps/Enemies)

    /**
     * Applies slowness debuff to the player (reduces speed to 40%).
     * Used by slowness traps and certain enemies.
     * Grants 0.5s invincibility when effect expires.
     *
     * @param duration how long the slow lasts in seconds
     */
    public void applySlow(float duration) {
        // If notSlowed and notInvincible
        if (this.slowTimer <= 0 && this.invincibilityTimer <= 0) {
            this.slowTimer = duration;
            VoiceManager.playTrap("trap_slow");
        }
    }

    /**
     * Applies root debuff to the player (completely prevents movement).
     * Used by vine traps that immobilize the player.
     * Grants 1.0s invincibility when effect expires (escape window).
     *
     * @param duration how long the root lasts in seconds
     */
    public void applyRoot(float duration) {

        // Apply if notRooted and notInvincible
        if (this.rootedTimer <= 0 && this.invincibilityTimer <= 0) {
            // Root down the player
            this.rootedTimer = duration;
            // Disable dashing
            this.isDashing = false;
            VoiceManager.playTrap("trap_root");
        }
    }

    // Getters & Setters for PowerUps

    /**
     * Adds lives to the player (healing).
     * Called by heart collectibles and mastery health regeneration.
     * Maximum health is capped at 20.
     *
     * @param amount number of lives to add (usually 1)
     */
    public void addLives(int amount) {
        // FIXED: Restored actual addition math and clamping logic
        this.lives += amount;
        if (this.lives > 20) {
            this.lives = 20;
        }
    }


    public int getLives() {
        return lives;
    }


    public void activateSpeedBoost(float duration) {
        this.speedBoostTimer = duration;
    }


    public boolean isSpeedBoostActive() {
        return speedBoostTimer > 0;
    }


    public float getCurrentStamina() {
        return currentStamina;
    }


    public float getMaxStamina() {
        return maxStamina;
    }

    // Getters/Setters for cheats
    public void setGhostMode(boolean active) {
        this.ghostMode = active;
    }


    public boolean isGhostMode() {
        return ghostMode;
    }


    public void setImmortal(boolean active) {
        this.permanentImmortal = active;
    }


    public boolean isImmortal() {
        return permanentImmortal;
    }


    public float getRootedTimer() {
        return rootedTimer;
    }


    public float getSlowTimer() {
        return slowTimer;
    }

    public float getSpeedBoostTimer() {
        return speedBoostTimer;
    }


    public float getDamageBoostTimer() {
        return damageBoostTimer;
    }


    public boolean isAttacking() {
        return attackTimer > 0;
    }

    /**
     * Gets the player's current weapon damage including all modifiers
     * That is when the player has the 2X Damage powerup
     *
     * @return total damage dealt by the player's attack
     */
    public int getWeaponDamage() {
        if (mastery.hasUpgrade(MasteryManager.ATTACK_4)) {
            return 9999; // one hit kill
        }

        int calculatedDamage = this.weaponDamage;

        // checking case 2 and 1, accordingly adding +damage to base damage
        if (mastery.hasUpgrade(MasteryManager.ATTACK_2)) {
            calculatedDamage += 2;
        } else if (mastery.hasUpgrade(MasteryManager.ATTACK_1)) {
            calculatedDamage += 1;
        }

        // 2x damage powerup
        if (damageBoostTimer > 0) {
            return calculatedDamage * 2;
        }

        return calculatedDamage;
    }




    public void activateDamageBoost(float duration) {
        this.damageBoostTimer = duration;
    }


    public boolean isDamageBoostActive() {
        return damageBoostTimer > 0;
    }

    public boolean isDashing() {
        return isDashing;
    }

    /**
     * Updates the player's base weapon damage.
     * Called when picking up weapon collectibles (stone sword, diamond sword, etc.)
     * Mastery upgrades are added on top of this base value.
     *
     * @param newDamage the new base damage value (1-3)
     */
    public void setWeaponDamage(int newDamage) {
        this.weaponDamage = newDamage;
        Gdx.app.log("Player", "Weapon upgraded! New damage: " + weaponDamage);
    }

    /**
     * Custom draw method with visual feedback for status effects.
     * Changes the player's tint color based on current state:
     *
     * @param batch the SpriteBatch to draw with
     */
    @Override
    public void draw(Batch batch) {
        float drawX = x;
        float drawY = y;
        float drawW = width;
        float drawH = height;

        // Visual Feedback (Red/Green/Gray tints)
        if (invincibilityTimer > 0.5f) {
            if (((int) (invincibilityTimer * 10)) % 2 == 0)
                batch.setColor(1, 0, 0, 0.5f);
        } else if (rootedTimer > 0) {
            // Pulses Green for root feedback
            float pulse = (float) Math.sin(stateTime * 15f) * 0.4f;
            batch.setColor(0.3f + pulse, 1f, 0.3f + pulse, 1f);
            drawX += com.badlogic.gdx.math.MathUtils.random(-1f, 1f);
        } else if (isDashing && mastery.hasUpgrade(MasteryManager.DASH_4)) {
            batch.setColor(0.5f, 1f, 1f, 0.8f); // apply a glow to signal that you dodged losing health
        } else if (slowTimer > 0) {
            batch.setColor(0.6f, 0.6f, 0.6f, 1f);
        } else if (speedBoostTimer > 0) {
            batch.setColor(1, 1, 0, 1);
        } else if (damageBoostTimer > 0) {
            float pulse = (float) Math.sin(stateTime * 12f) * 0.3f;
            batch.setColor(1f, 0.6f + pulse, 0.2f, 1f);
        }

        if (textureRegion != null) {
            batch.draw(textureRegion, drawX, drawY, drawW, drawH);
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * Checks if the death animation has finished playing.
     * Used by GameScreen to trigger game over screen after animation completes.
     * Death animation lasts 1 second before game over.
     *
     * @return true if player is dead and animation has finished
     */
    public boolean isDeathAnimationFinished() {
        return lives <= 0 && timeSinceDeath >= 1.0f;
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.bounds.setPosition(x, y);
    }

}
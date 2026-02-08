/**
 * The Enemy class is responsible for the actual movement of each Enemy,
 * this class inherits from the (@link Obstacle) class and provides its own version
 * of the update() method, where we apply the logic for the movement of the Enemy
 * Enemies have two states:
 *  - PATROL: Wandering around their spawn point
 *  - ATTACK: Actively chasing the player using A* pathfinding
 * @author Waleed :> and everyone else
 */

package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import java.util.List;


public class Enemy extends Obstacle {

    // State variables
    // Patrol state refers to enemy not doing anything/wandering around
    // Attack state is when they are actively chasing the enemy
    protected enum State {
        PATROL, ATTACK
    }

    // The enemy's current AI behavior mode
    protected State currentState = State.PATROL;

    // Combat variables
    protected int health;
    private boolean isDead = false;
    protected boolean isDying = false; // New state for death animation
    protected float hurtTimer = 0f;

    // Death animation variables
    protected float deathTimer = 0f;
    protected float deathDuration = 0.5f; // Default duration, can be overridden by subclasses

    // --- Knockback Variables ---
    // When hit, the enemy gets pushed back for a short time
    private float knockbackTimer = 0f;
    private float kbX = 0, kbY = 0;
    private final float KNOCKBACK_SPEED = 110f;
    private final float KNOCKBACK_DURATION = 0.08f;

    private List<int[]> currentPath;
    private float pathUpdateTimer = 0;

    private float spawnX, spawnY;
    private float targetX, targetY;

    // Whether the enemy is currently moving toward a target tile
    protected boolean isMoving = false;

    private float baseSpeed;
    private float damageValue;
    private float detectionRadius;
    private float idleTimer = 0;

    /**
     * Creates a new enemy with the specified stats and position.
     *
     * @param x       starting x coordinate in world units
     * @param y       starting y coordinate in world units
     * @param width   width of the enemy sprite
     * @param height  height of the enemy sprite
     * @param texture the image to display for this enemy
     * @param speed   movement speed (different for each enemy type)
     * @param radius  detection radius (how far the enemy can see the player)
     * @param damage  damage dealt to the player on contact
     * @param health  number of hits this enemy can take before dying
     */
    public Enemy(float x, float y, float width, float height, TextureRegion texture,
                 float speed, float radius, int damage, int health) {
        super(x, y, width, height, texture);
        this.spawnX = x;
        this.spawnY = y;
        this.targetX = x;
        this.targetY = y;
        this.baseSpeed = speed;
        this.detectionRadius = radius;
        this.damageValue = damage;
        this.health = health;
        // Make the hitbox slightly smaller than the sprite for better feel
        this.bounds.setSize(14f, 14f);
    }

    /**
     * Called when the player's weapon hits this enemy.
     * Reduces health, applies knockback, plays sound, and starts death animation if health reaches zero.
     *
     * @param damage  the amount of health to subtract
     * @param sourceX the x position of the attack source (for knockback direction)
     * @param sourceY the y position of the attack source (for knockback direction)
     */
    public void takeHit(int damage, float sourceX, float sourceY) {
        if (isDead || isDying || hurtTimer > 0)
            return;

        this.health -= damage;
        this.hurtTimer = 0.2f;
        Gdx.app.log("Enemy", "Took " + damage + " damage. Health left: " + health);

        // --- CALCULATE KNOCKBACK DIRECTION ---
        // We find the vector from the player to the enemy
        float dx = this.x - sourceX;
        float dy = this.y - sourceY;

        // Normalize the vector (make it length 1) so the push is consistent
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance != 0) {
            this.kbX = dx / distance;
            this.kbY = dy / distance;
        }
        this.knockbackTimer = KNOCKBACK_DURATION;

        if (this.health <= 0) {
            this.isDying = true; // Start the death animation
            this.deathTimer = 0f;
            // Note: We do NOT set isDead = true here yet.
            // It will be set in update() after the animation duration.

            // Play enemy death sound
            VoiceManager.playEnemy("enemy_normal_die");

            Gdx.app.log("Enemy", "Enemy defeated! Playing death animation...");
        }
    }

    /**
     * Checks if this enemy is completely dead and ready to be removed.
     *
     * @return true if the death animation has finished
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * Checks if this enemy is currently playing its death animation.
     *
     * @return true if dying but not yet fully dead
     */
    public boolean isDying() {
        return isDying;
    }

    /**
     * Updates the enemy's AI, movement, and combat logic every frame.
     * Handles death animation, knockback, state switching (patrol vs attack),
     * pathfinding, and collision with the player.
     *
     * @param delta      time passed since last frame in seconds
     * @param player     reference to the player for targeting and collision
     * @param mapManager reference to the map for pathfinding and wall collision
     */
    @Override
    public void update(float delta, Player player, MapManager mapManager) {
        // Don't do anything if the enemy is fully dead
        if (isDead)
            return;

        // handle the dying state
        if (isDying) {
            deathTimer += delta;
            // Once the animation duration is over, mark as fully dead
            if (deathTimer >= deathDuration) {
                isDead = true; // mark for removal after animation finishes
            }
            return; // stop movement and attack logic while dying
        }

        int tileSize = mapManager.getTileSize();

        // counting down the hurt timer every frame
        if (hurtTimer > 0)
            hurtTimer -= delta;

        // knockback logic
        if (knockbackTimer > 0) {
            float oldX = x;
            float oldY = y;

            // move the enemy in the knockback direction
            x += kbX * KNOCKBACK_SPEED * delta;
            y += kbY * KNOCKBACK_SPEED * delta;

            setPosition(x, y);

            // if we hit a wall, stop the knockback and move back
            if (mapManager.isCollidingWithWall(getBounds())) {
                x = oldX;
                y = oldY;
                setPosition(x, y);
                knockbackTimer = 0;
            }

            knockbackTimer -= delta;
            isMoving = false; // interrupt current pathfinding
            return; // just return cause we hit the wall
        }

        State previousState = currentState; // store the previous state of the enemy

        // using Pythagoras' theorem three to calculate distance from the enemy to
        // player
        // c = root of a2 + b2
        // think of it visually, imagine an enemy and a player, and make a right-angled
        // triangle between them
        // maybe make some visualization for the presentation
        float dist = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2));

        // now here, we check for the distance, and then switch the states
        if (dist < detectionRadius)
            currentState = State.ATTACK; // if player is inside the circle, attack
        else
            currentState = State.PATROL; // otherwise, stay on patrol

        if (previousState == State.PATROL && currentState == State.ATTACK) {
            isMoving = false; // break the current movement loop
            currentPath = null; // clear the old patrol destination
            pathUpdateTimer = 0.6f; // immediate pathfinding check in the next block
        }

        // if the current state is attack, make the current speed 0.6 * bases peed, else
        // keep current speed
        // the base speed (i.e. they aren't attacking and are on patrol
        float currentSpeed = (currentState == State.ATTACK) ? baseSpeed : baseSpeed * 0.6f;

        // now this here is the main part of the enemies class, here when our enemy is
        // completely
        // still or has reached its destination.
        if (!isMoving) {
            pathUpdateTimer += delta;

            if (currentState == State.ATTACK) {
                // if we're attacking
                if (pathUpdateTimer > 0.5f) {
                    // if it's been more than 0.5s
                    currentPath = AI.findPath((int) (x / tileSize), (int) (y / tileSize),
                            (int) (player.getX() / tileSize), (int) (player.getY() / tileSize),
                            mapManager.getMapData());
                    // calculate logic, we also getMapData() because we want our enemies to avoid
                    // collisions
                    pathUpdateTimer = 0;
                    // reinitialize the datetimepicker to 0
                }
            } else {
                // we are not attacking, still on patrol
                idleTimer += delta;
                if (idleTimer > 2.0f) {
                    // if we've been waiting for more than 2 seconds
                    int rx = (int) (spawnX / tileSize) + MathUtils.random(-3, 3);
                    int ry = (int) (spawnY / tileSize) + MathUtils.random(-3, 3);
                    // converting pixel position into a tile positon, since tile size is 16, we
                    // divide by 16,
                    // if 32, we divide by 32 and so on
                    // thus here, we get random coords for which, our enemies will wander to
                    // here, we have the limit of 3 tile radius, i.e., they wander maximum 3 tiles
                    // form their position in any directory
                    if (rx >= 0 && rx < mapManager.getWidth() && ry >= 0 && ry < mapManager.getHeight()) {
                        if (mapManager.getMapData()[ry][rx] == 0) {
                            // the above two if statements make sure we don't run into the arrayoutofbound
                            // exceptions again
                            // this basically fixes the problem that potentially, the random tile
                            // coordinates could be pointing outside
                            // the map, or they could be pointing at a wall, so we make sure that
                            // if, the random coordinates are scontained withing the map, and if the mapdata
                            // for those coordinates
                            // gives us a path, then we run the code below, which is the pathfinding code to
                            // the random coords
                            currentPath = AI.findPath((int) (x / tileSize), (int) (y / tileSize), rx, ry,
                                    mapManager.getMapData());
                        }
                    }
                    idleTimer = 0;
                }
            }

            // If we have a path, grab the next tile to move toward
            if (currentPath != null && !currentPath.isEmpty()) {
                int[] next = currentPath.remove(0);
                targetX = next[0] * tileSize;
                targetY = next[1] * tileSize;
                isMoving = true;
            }
        }

        // Movement Interpolation
        if (isMoving) {
            float step = currentSpeed * delta;
            if (x < targetX)
                x = Math.min(x + step, targetX);
            else if (x > targetX)
                x = Math.max(x - step, targetX);

            if (y < targetY)
                y = Math.min(y + step, targetY);
            else if (y > targetY)
                y = Math.max(y - step, targetY);

            setPosition(x, y);

            // Precision check for arrival using Epsilon
            float errorMargin = 0.1f;
            if (Math.abs(x - targetX) < errorMargin && Math.abs(y - targetY) < errorMargin) {
                x = targetX; // Snap to exact position
                y = targetY;
                isMoving = false;
            }
            // when we arrive at your target, make our character stop moving, we implement
            // the error margin to make the
            // arrival preciser (is that a word?)
        }

        // here we damage the player, the contactwithplayer confirms the bounds
        // and if it returns true, damage the player
        if (contactWithPlayer(player)) {
            player.takeDamage((int) damageValue);
        }
    }

    /**
     * Adds bonus health to this enemy for scaling difficulty in endless mode.
     *
     * @param bonus the amount of extra health to add
     */
    public void addExtraHealth(int bonus) {
        this.health += bonus;
        Gdx.app.log("Enemy", "Endless Scaling: HP increased to " + this.health);
    }

    /**
     * Draws the enemy on screen with a red tint if recently hit.
     *
     * @param batch the LibGDX batch used for rendering
     */
    @Override
    public void draw(Batch batch) {
        if (isDead)
            return;

        // If recently hit, tint the enemy red
        if (hurtTimer > 0) {
            batch.setColor(1, 0.3f, 0.3f, 1);
        }

        super.draw(batch); // Call GameObject.draw
        batch.setColor(Color.WHITE); // Reset for other objects
    }
}

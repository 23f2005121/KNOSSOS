package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Base class for all visible game objects that have a position, size, texture, and collision box.
 * Everything in the game that can be drawn and collided with inherits from this.
 * This includes the player, enemies, walls, items, traps, and more.
 */
public abstract class GameObject {

    // World position of the object (bottom-left corner in LibGDX coordinate system)
    protected float x, y;

    // Physical dimensions of the object in world units
    protected float width, height;

    // Invisible rectangle used for collision detection (same size as the object)
    protected Rectangle bounds;

    // The actual image/sprite that gets drawn for this object
    protected TextureRegion textureRegion;

    /**
     * Creates a new GameObject with position, size, and an initial texture.
     * Also creates a collision rectangle (bounds) that matches the object's dimensions.
     *
     * @param x             starting x position in world units
     * @param y             starting y position in world units
     * @param width         width of the object
     * @param height        height of the object
     * @param textureRegion the image to display for this object
     */
    public GameObject(float x, float y, float width, float height, TextureRegion textureRegion) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textureRegion = textureRegion;
        // Create the collision box at the same position and size as the visual object
        this.bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Renders the texture of the object at its current position.
     * This is called every frame to draw the object on screen.
     * Using batch.draw() is efficient because LibGDX collects all draw calls
     * and sends them to the GPU in one batch, rather than individually.
     *
     * @param batch the LibGDX sprite batch used for rendering
     */
    public void draw(Batch batch) {
        batch.draw(textureRegion, x, y, width, height);
    }

    /**
     * Returns the collision rectangle for this object.
     * Other classes use this to check if objects are overlapping
     * (for example, player touching an item, or enemy hitting the player).
     *
     * @return the Rectangle representing this object's hitbox
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Updates the object's position and moves the collision rectangle to match.
     * Important: This keeps the visual position and the hitbox in sync.
     * If you only update x and y without calling this, collisions won't work correctly.
     *
     * @param x new x coordinate
     * @param y new y coordinate
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        // Move the collision box to the new position so it stays aligned with the image
        this.bounds.setPosition(x, y);
    }

    /**
     * Gets the current x position in world coordinates.
     *
     * @return the x coordinate
     */
    public float getX() { return x; }

    /**
     * Gets the current y position in world coordinates.
     *
     * @return the y coordinate
     */
    public float getY() { return y; }
}

package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;

/**
 * Central manager for all map-related functionality in the game.
 * Handles level loading, rendering, collision detection, and line of sight calculations.
 * <p>
 * Responsibilities:
 * - Loading levels from story mode files or generating random mazes for endless mode
 * - Managing wall and floor textures with automatic tile connections (autotiling)
 * - Rendering the map with walls, floors, and decorative props
 * - Checking collisions between entities and walls
 * - Calculating line of sight for enemy AI (can mage shoot through walls?)
 * - Providing random walkable positions for spawning items and enemies
 * <p>
 * The map is represented as a 2D grid where:
 * - 0 = walkable floor tile
 * - 1 = solid wall tile
 * <p>
 * Uses a bitmask autotiling system to make walls connect seamlessly.
 */
public class MapManager {

    // Current story level data (null in endless mode)
    private StoryLevelLoader.LevelData currentLevelInfo;

    // 2D grid representing the map (0 = floor, 1 = wall)
    private int[][] mapData;

    // 2D grid storing decorative prop indices at each position (0 = no prop)
    private int[][] propMap;

    // Map dimensions in tiles
    private int width, height;

    // Size of each tile in pixels
    private final int TILE_SIZE = 16;

    // Wall tile textures (16 variations for all connection combinations)
    private TextureRegion[] wallTiles;
    private Texture wallTextureSheet;

    // Floor texture
    private Texture pathTexture;

    // Decorative prop assets (plants, rocks, etc.)
    private Texture propsTextureSheet;
    private TextureRegion[] propTiles;

    // Special tile positions for story mode (in grid coordinates)
    public int startX, startY;  // Player spawn position
    public int keyX, keyY;      // Key spawn position
    public int exitX, exitY;    // Exit door position

    /**
     * Creates a new MapManager and loads all texture assets.
     * Initializes wall tiles, floor tiles, and decorative props.
     */
    public MapManager() {
        loadTextures();
    }

    /**
     * Loads all texture assets for walls, floors, and props.
     * Falls back to solid color textures if any asset files are missing.
     * <p>
     * Wall tiles use a 4x4 grid (16 tiles) for autotiling.
     * Props use a 1x7 grid (7 different decorations).
     */
    private void loadTextures() {
        // Load wall tileset
        try {
            wallTextureSheet = new Texture(Gdx.files.internal("walls.png"));
            // Split the 64x64 walls texture into a 4x4 grid (16 tiles of 16x16 each)
            TextureRegion[][] tmp = TextureRegion.split(wallTextureSheet, TILE_SIZE, TILE_SIZE);
            wallTiles = new TextureRegion[16];
            int index = 0;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    wallTiles[index++] = tmp[i][j];
                }
            }
        } catch (Exception e) {
            // If walls.png is missing, create a gray fallback texture
            wallTiles = new TextureRegion[16];
            Texture temp = createColorTexture(Color.GRAY);
            for (int i = 0; i < 16; i++) {
                wallTiles[i] = new TextureRegion(temp);
            }
        }

        // Load floor texture
        try {
            pathTexture = new Texture(Gdx.files.internal("path.png"));
        } catch (Exception e) {
            // If path.png is missing, use a green fallback color
            System.err.println("path.png not found, using default color.");
            pathTexture = createColorTexture(Color.valueOf("557D55"));
        }

        // Load decorative props
        try {
            if (Gdx.files.internal("props.png").exists()) {
                propsTextureSheet = new Texture(Gdx.files.internal("props.png"));
                // 224x32 pixels = 7 columns of 32x32 props
                TextureRegion[][] tmp = TextureRegion.split(propsTextureSheet, 32, 32);
                propTiles = new TextureRegion[7];
                for (int i = 0; i < 7; i++) {
                    if (i < tmp[0].length) {
                        propTiles[i] = tmp[0][i];
                    }
                }
            } else {
                propTiles = new TextureRegion[0];
            }
        } catch (Exception e) {
            System.err.println("props.png could not be loaded: " + e.getMessage());
            propTiles = new TextureRegion[0];
        }
    }

    /**
     * Creates a simple solid-color texture for fallback purposes.
     * Used when asset files are missing.
     *
     * @param c the color to fill the texture with
     * @return a 16x16 solid color texture
     */
    private Texture createColorTexture(Color c) {
        Pixmap p = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    /**
     * Loads a level into the map.
     * In story mode, loads a pre-designed level from a file.
     * In endless mode, generates a random procedural maze.
     * Also, randomly places decorative props on floor tiles.
     *
     * @param level       the level number to load
     * @param isStoryMode true for story mode (fixed layouts), false for endless mode (random)
     */
    public void loadLevel(int level, boolean isStoryMode) {
        if (isStoryMode) {
            // Load pre-designed level from properties file
            this.currentLevelInfo = StoryLevelLoader.loadLevel(level);
            this.mapData = currentLevelInfo.grid;

            this.width = currentLevelInfo.width;
            this.height = currentLevelInfo.height;

            // Store positions of key game objects
            this.startX = currentLevelInfo.startX;
            this.startY = currentLevelInfo.startY;

            this.keyX = currentLevelInfo.keyX;
            this.keyY = currentLevelInfo.keyY;

            this.exitX = currentLevelInfo.exitX;
            this.exitY = currentLevelInfo.exitY;
        } else {
            // Generate a random maze for endless mode
            MazeMap gen = new MazeMap();
            this.mapData = gen.getMap();
            this.width = mapData[0].length;
            this.height = mapData.length;
            currentLevelInfo = null;
        }

        // Generate decorative props randomly on floor tiles
        this.propMap = new int[height][width];
        if (propTiles != null && propTiles.length > 0) {
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    // Only place props on floor tiles (0)
                    if (mapData[r][c] == 0) {
                        // Don't cover important tiles in story mode
                        boolean reserved = false;
                        if (isStoryMode) {
                            if (c == startX && r == startY) reserved = true;
                            if (c == keyX && r == keyY) reserved = true;
                            if (c == exitX && r == exitY) reserved = true;
                        }

                        if (!reserved) {
                            // 2% chance to place a random prop
                            if (MathUtils.randomBoolean(0.02f)) {
                                propMap[r][c] = MathUtils.random(1, 7);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders the entire map (floors, props, and walls) to the screen.
     * Uses autotiling to make wall tiles connect seamlessly.
     * <p>
     * Rendering order:
     * 1. Floor tiles (everywhere)
     * 2. Decorative props (on some floor tiles)
     * 3. Walls (using bitmask autotiling)
     *
     * @param batch  the sprite batch to draw with
     * @param camera the camera (currently unused but could be used for culling)
     */
    public void render(Batch batch, com.badlogic.gdx.graphics.Camera camera) {
        // Loop through every tile in the map grid
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                // Convert grid coordinates to pixel coordinates
                float dx = col * TILE_SIZE;
                float dy = row * TILE_SIZE;

                // Draw floor tile (always present)
                batch.draw(pathTexture, dx, dy, TILE_SIZE, TILE_SIZE);

                // Draw decorative prop if one exists at this position
                if (propMap != null && propMap[row][col] > 0) {
                    int propIndex = propMap[row][col] - 1; // Convert to 0-based index
                    if (propTiles != null && propIndex < propTiles.length) {
                        batch.draw(propTiles[propIndex], dx, dy, TILE_SIZE, TILE_SIZE);
                    }
                }

                // Draw wall tile if this is a wall
                if (mapData[row][col] == 1) {
                    // Use bitmask to determine which wall sprite to use
                    batch.draw(wallTiles[calculateBitmask(col, row)], dx, dy, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    /**
     * Calculates the autotiling bitmask for a wall tile.
     * The bitmask encodes which neighboring tiles are also walls.
     * This determines which sprite variation to use for seamless connections.
     * <p>
     * Bitmask encoding:
     * - North wall: +1
     * - East wall:  +2
     * - South wall: +4
     * - West wall:  +8
     * <p>
     * Result is an integer 0-15 that maps to the corresponding tile in the tileset.
     *
     * @param col the column (x) of the tile
     * @param row the row (y) of the tile
     * @return a bitmask value 0-15 indicating which neighbors are walls
     */
    private int calculateBitmask(int col, int row) {
        int mask = 0;
        // Check each cardinal direction and add to the mask if it's a wall
        if (isWallGrid(col, row + 1)) {
            mask += 1;  // North
        }
        if (isWallGrid(col + 1, row)) {
            mask += 2;  // East
        }
        if (isWallGrid(col, row - 1)) {
            mask += 4;  // South
        }
        if (isWallGrid(col - 1, row)) {
            mask += 8;  // West
        }
        return mask;
    }

    /**
     * Checks if a specific grid position contains a wall.
     * Returns false for positions outside the map bounds (treated as floor).
     *
     * @param c column (x coordinate in grid)
     * @param r row (y coordinate in grid)
     * @return true if the tile is a wall, false otherwise
     */
    private boolean isWallGrid(int c, int r) {
        if (r < 0 || r >= height || c < 0 || c >= width) {
            return false;
        }
        return mapData[r][c] == 1;
    }

    /**
     * Checks if an entity's bounding box is colliding with any walls.
     * Uses optimized grid checking - only tests tiles near the entity.
     * Handles special hitboxes for certain wall tiles (some are narrower than 16x16).
     *
     * @param entityRect the entity's collision rectangle
     * @return true if the entity is overlapping any wall, false otherwise
     */
    public boolean isCollidingWithWall(Rectangle entityRect) {
        // Determine which grid cells the entity could be touching
        int minX = (int) (entityRect.x / TILE_SIZE);
        // Epsilon buffer ensures we don't snag on tiles we are just touching
        int maxX = (int) ((entityRect.x + entityRect.width - 0.01f) / TILE_SIZE);
        int minY = (int) (entityRect.y / TILE_SIZE);
        int maxY = (int) ((entityRect.y + entityRect.height - 0.01f) / TILE_SIZE);

        // Check only the relevant tiles
        for (int row = minY; row <= maxY; row++) {
            for (int col = minX; col <= maxX; col++) {
                // Bounds check to prevent crashes at map edges
                if (col >= 0 && col < width && row >= 0 && row < height) {
                    // If this tile is a wall, check its specific hitbox
                    if (mapData[row][col] == 1) {
                        Rectangle wallBox = getWallHitbox(col, row);
                        // Check if entity rectangle overlaps this wall's hitbox
                        if (entityRect.overlaps(wallBox)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if there's a clear line of sight between two points.
     * Used by mage enemies to determine if they can shoot at the player.
     * Samples points along the line and checks if any pass through walls.
     *
     * @param startX  starting x position in world coordinates
     * @param startY  starting y position in world coordinates
     * @param targetX target x position in world coordinates
     * @param targetY target y position in world coordinates
     * @return true if no walls block the line, false if any wall is in the way
     */
    public boolean hasLineOfSight(float startX, float startY, float targetX, float targetY) {
        float dx = targetX - startX;
        float dy = targetY - startY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Sample the line every 4 pixels
        int steps = (int) (distance / 4);

        // Check each sample point for wall collision
        for (int i = 0; i < steps; i++) {
            float checkX = startX + (dx * i / steps);
            float checkY = startY + (dy * i / steps);

            // If any sample point is inside a wall, line of sight is blocked
            if (isWallAt(checkX, checkY)) {
                return false;
            }
        }
        return true; // No walls found along the line
    }

    /**
     * Gets the collision hitbox for a specific wall tile.
     * Some wall tiles use narrower hitboxes (8 pixels wide instead of 16)
     * to allow the player to squeeze past corners more easily.
     * Which tiles get narrow hitboxes is determined by their bitmask.
     *
     * @param col the wall tile's column
     * @param row the wall tile's row
     * @return a Rectangle representing the wall's collision area
     */
    private Rectangle getWallHitbox(int col, int row) {
        int mask = calculateBitmask(col, row);
        float tileX = col * TILE_SIZE;
        float tileY = row * TILE_SIZE;

        // determine which tiles need the center aligning
        switch (mask) {
            case 0: case 1: case 2: case 3:
            case 4: case 5: case 6: case 7:
            case 8: case 9:
            case 12: case 13:
                // Return a centered 8-pixel wide strip
                return new Rectangle(tileX + 4, tileY, 8, TILE_SIZE);

            default:
                return new Rectangle(tileX, tileY, TILE_SIZE, TILE_SIZE);
        }
    }

    /**
     * Checks if a specific pixel coordinate contains a wall.
     * Converts world coordinates to grid coordinates and checks the map data.
     *
     * @param x x position in world coordinates (pixels)
     * @param y y position in world coordinates (pixels)
     * @return true if there's a wall at that position, false otherwise
     */
    private boolean isWallAt(float x, float y) {
        // Convert pixel coordinates to grid coordinates
        int gx = (int) (x / TILE_SIZE);
        int gy = (int) (y / TILE_SIZE);

        // Treat out-of-bounds positions as walls (prevents entities from escaping map)
        if (gx < 0 || gx >= width || gy < 0 || gy >= height) {
            return true;
        }
        return mapData[gy][gx] == 1;
    }

    /**
     * Finds a random walkable floor tile on the map.
     * Keeps trying random positions until it finds one that isn't a wall.
     * Used for spawning items, enemies, and the exit in endless mode.
     *
     * @return array containing [x, y] in world coordinates (pixels)
     */
    public float[] getRandomFloorPosition() {
        while (true) {
            // Pick a random position (avoiding edges to prevent spawning at borders)
            int r = MathUtils.random(1, height - 2);
            int c = MathUtils.random(1, width - 2);

            // If it's a floor tile, return its position in pixels
            if (mapData[r][c] == 0) {
                return new float[] { c * TILE_SIZE, r * TILE_SIZE };
            }
            // Otherwise, try again
        }
    }

    /**
     * Gets the raw map grid data.
     *
     * @return 2D array where 0 = floor, 1 = wall
     */
    public int[][] getMapData() {
        return mapData;
    }

    /**
     * Gets the map width in tiles.
     *
     * @return number of columns in the map
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the map height in tiles.
     *
     * @return number of rows in the map
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the size of each tile in pixels.
     *
     * @return tile size (always 16)
     */
    public int getTileSize() {
        return TILE_SIZE;
    }

    /**
     * Gets the current story level data (null in endless mode).
     *
     * @return the loaded level data, or null if in endless mode
     */
    public StoryLevelLoader.LevelData getLevelInfo() {
        return currentLevelInfo;
    }

    /**
     * Cleans up all loaded texture assets to prevent memory leaks.
     * Should be called when the map is no longer needed.
     */
    public void dispose() {
        if (wallTextureSheet != null) {
            wallTextureSheet.dispose();
        }
        if (pathTexture != null) {
            pathTexture.dispose();
        }
        if (propsTextureSheet != null) {
            propsTextureSheet.dispose();
        }
    }
}

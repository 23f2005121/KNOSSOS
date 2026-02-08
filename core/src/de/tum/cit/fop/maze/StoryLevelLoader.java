package de.tum.cit.fop.maze;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Loads story mode levels from external .properties files.
 * Parses map data and entity positions into a usable LevelData structure for MapManager.

 */
public class StoryLevelLoader {

    /**
     * Data Transfer Object (DTO) that transports level information from file to game engine.
     * Contains all information needed to construct a playable level:

     * All entity positions are stored as separate coordinates rather than
     * in the grid to make entity spawning easier in MapManager.
     */
    public static class LevelData {
        // 2D array representing the map terrain (0=path, 1=wall)
        public int[][] grid;

        // Map dimensions in tiles
        public int width, height;

        // Player spawn position in tile coordinates
        public int startX, startY;

        // Exit position in tile coordinates (goal of the level)
        public int exitX, exitY;

        // Key position in tile coordinates (required to unlock exit)
        public int keyX, keyY;

        // List of trap positions [x, y]
        // Multiple traps can exist per level
        public List<int[]> traps = new ArrayList<>();

        // List of enemy spawn positions [x, y]
        // Multiple enemies can exist per level
        public List<int[]> enemies = new ArrayList<>();
    }

    /**
     * Loads a story mode level from a .properties file.
     * Reads maps/level-N.properties and parses all map data into a LevelData object.

     *
     * @param levelNumber the story level number to load (1-based)
     * @return LevelData containing all map information, or empty grid if file not found
     */
    public static LevelData loadLevel(int levelNumber) {
        // Create empty data structure to populate
        LevelData data = new LevelData();

        // Construct file path: maps/level-1.properties, maps/level-2.properties, etc.
        String fileName = "maps/level-" + levelNumber + ".properties";
        File file = new File(fileName);

        // FALLBACK: If level file doesn't exist, return empty 32×32 grid
        // This prevents crashes when requesting non-existent levels
        if (!file.exists()) {
            data.width = 32;
            data.height = 32;
            data.grid = new int[32][32]; // Defaults to all 0s (paths)
            return data;
        }

        // Java Properties object for parsing key=value format
        // Properties handles the file parsing and splits lines by '='
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            // Load and parse the entire file
            // Each line "x,y=value" becomes a property entry
            props.load(fis);
        } catch (IOException e) {
            // FALLBACK: If file can't be read, return empty 32×32 grid
            e.printStackTrace();
            data.grid = new int[32][32];
            return data;
        }

        // ===== DETERMINE MAP DIMENSIONS =====
        // Two methods:
        // 1. Explicit Width/Height properties (preferred)
        // 2. Scan all coordinates and find maximum x,y (fallback)

        int maxX = 0, maxY = 0;

        // Method 1: Read explicit Width and Height if present
        try {
            if (props.containsKey("Width")) {
                // Width is stored as 1-based count, convert to 0-based max index
                maxX = Integer.parseInt(props.getProperty("Width").trim()) - 1;
            }
            if (props.containsKey("Height")) {
                // Height is stored as 1-based count, convert to 0-based max index
                maxY = Integer.parseInt(props.getProperty("Height").trim()) - 1;
            }
        } catch (Exception e) {
            // If parsing fails, fall through to coordinate scanning
        }

        // Method 2: Scan all coordinate keys to find the largest x and y
        // This ensures the grid is large enough even if Width/Height are missing
        for (String key : props.stringPropertyNames()) {
            // Skip the Width and Height keys (not coordinates)
            if (key.equals("Width") || key.equals("Height")) {
                continue;
            }
            try {
                // Parse coordinate key "x,y"
                String[] parts = key.split(",");
                if (parts.length == 2) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());

                    // Track maximum coordinates seen
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            } catch (Exception e) {
                // Ignore malformed keys (not a valid coordinate)
            }
        }

        // Set final dimensions (add 1 to convert from max index to size)
        data.width = maxX + 1;
        data.height = maxY + 1;

        // Allocate grid array: grid[y][x] for row-major storage
        data.grid = new int[data.height][data.width];
        // Default initialization: All cells = 0 (paths)

        // ===== PARSE MAP TILES AND ENTITIES =====
        // Loop through all coordinate entries and populate the grid/entity lists
        for (String key : props.stringPropertyNames()) {
            // Skip metadata keys
            if (key.equals("Width") || key.equals("Height")) {
                continue;
            }

            try {
                // Parse coordinate "x,y"
                String[] parts = key.split(",");
                if (parts.length == 2) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());

                    // Parse tile type value
                    int value = Integer.parseInt(props.getProperty(key).trim());

                    // Bounds check: Only process valid coordinates
                    if (x >= 0 && x < data.width && y >= 0 && y < data.height) {
                        // Handle tile type based on ID
                        switch (value) {
                            case 0: // WALL
                                // Set grid cell to 1 (solid wall)
                                // Only if not already set (prevents overwriting)
                                if (data.grid[y][x] == 0) {
                                    data.grid[y][x] = 1;
                                }
                                break;

                            case 1: // ENTRY (player spawn)
                                // Mark as path (walkable)
                                data.grid[y][x] = 0;
                                // Record spawn position
                                data.startX = x;
                                data.startY = y;
                                break;

                            case 2: // EXIT (level goal)
                                // Mark as path (walkable)
                                data.grid[y][x] = 0;
                                // Record exit position
                                data.exitX = x;
                                data.exitY = y;
                                break;

                            case 3: // TRAP
                                // Mark as path (traps don't block movement)
                                data.grid[y][x] = 0;
                                // Add to trap list for spawning
                                data.traps.add(new int[]{x, y});
                                break;

                            case 4: // ENEMY
                                // Mark as path (enemies move around)
                                data.grid[y][x] = 0;
                                // Add to enemy list for spawning
                                data.enemies.add(new int[]{x, y});
                                break;

                            case 5: // KEY
                                // Mark as path (key is a collectible)
                                data.grid[y][x] = 0;
                                // Record key position
                                data.keyX = x;
                                data.keyY = y;
                                break;

                            default: // EMPTY PATH
                                // Any other value = walkable path
                                data.grid[y][x] = 0;
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore malformed entries (invalid coordinates or values)
                // Allows rest of level to load even if some entries are broken
            }
        }

        // Return fully populated level data
        return data;
    }
}

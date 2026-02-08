package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;

/**
 * Manages saving and loading game state to disk using JSON files.
 * Players can save their progress in story mode and resume later from the same point.
 * Each save slot is stored as a separate JSON file (slot1.json, slot2.json, etc.).
 */
public class GameStateManager {

    /**
     * Represents a complete snapshot of the game state at a moment in time.
     * Contains everything needed to restore the game exactly as it was when saved:
     * level, score, player state, enemy positions, items on the ground, etc.
     */
    public static class SaveState {
        // Basic level info
        public int level;           // Which level the player is on
        public boolean isStoryMode; // Story mode vs endless mode

        // Score tracking
        public int totalScore;          // Cumulative score across all completed levels
        public float levelBaseScore;    // Base score countdown for current level
        public int sessionBonus;        // Bonus points earned in current level

        // Player state
        public float playerX, playerY;  // Player's position in the world
        public int playerLives;         // How many lives the player has left
        public float currentStamina;    // Current stamina value
        public int weaponDamage;        // Weapon upgrade level (1, 2, or 3)
        public float timeSinceDeath;    // Time since last death (prevents death loop on load)

        // Inventory and active buffs
        public boolean hasKey;          // Whether the player already collected the key
        public boolean hasSpeedBoost;   // Whether speed boost is active
        public boolean hasShield;       // Whether shield is active
        public boolean hasDamageBoost;  // Whether damage boost is active

        // All entities in the level
        public ArrayList<EnemyData> enemies = new ArrayList<>();  // All enemies and their positions
        public ArrayList<TrapData> traps = new ArrayList<>();     // All traps and their positions
        public ArrayList<ItemData> items = new ArrayList<>();     // Hearts, powerups, weapons on the ground
        public ItemData keyData;   // The key item (stored separately since there's only one)
        public ItemData exitData;  // The exit door (stored separately since there's only one)
    }

    /**
     * Stores information about a single enemy in the level.
     * Used to recreate enemies when loading a save.
     */
    public static class EnemyData {
        public String type; // Enemy type: "NORMAL", "BIG", "JOCKEY", "MAGE", or "SNAIL"
        public float x, y;  // Position in the world
        public int health;  // Remaining health points
    }

    /**
     * Stores information about a single trap in the level.
     * Used to recreate traps when loading a save.
     */
    public static class TrapData {
        public int type;    // Trap type (0, 1, or 2 for different trap sprites)
        public float x, y;  // Position in the world
    }

    /**
     * Stores information about a collectible item on the ground.
     * Can represent hearts, powerups, weapons, or special items like the key/exit.
     */
    public static class ItemData {
        public String type; // Item type: "HEART", "SPEED", "SHIELD", "DAMAGE", "WEAPON", "KEY", "EXIT"
        public float x, y;  // Position in the world
        public boolean active; // Whether the item is still on the ground (false if already collected)

        // Type-specific data
        public float duration; // How long the powerup lasts (for powerups only)
        public int value;      // Life restore amount (for hearts) or damage value (for weapons)
    }

    /**
     * Saves the current game state to a JSON file on disk.
     * The file is named "slotX.json" where X is the slot number (1, 2, or 3).
     * Overwrites any existing save in that slot.
     *
     * @param slot  the save slot number (1, 2, or 3)
     * @param state the complete game state to save
     */
    public static void saveGame(int slot, SaveState state) {
        // Create a JSON serializer
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json); // Human-readable format

        // Get the file handle for this save slot
        FileHandle file = Gdx.files.local("slot" + slot + ".json");

        // Write the game state as formatted JSON
        file.writeString(json.prettyPrint(state), false); // false means overwrite

        Gdx.app.log("SaveSystem", "Game saved to slot " + slot);
    }

    /**
     * Loads a saved game state from disk.
     * Returns null if the save file doesn't exist or is corrupted.
     *
     * @param slot the save slot number to load from (1, 2, or 3)
     * @return the loaded game state, or null if loading failed
     */
    public static SaveState loadGame(int slot) {
        FileHandle file = Gdx.files.local("slot" + slot + ".json");

        // Check if the save file exists
        if (!file.exists()) return null;

        // Try to parse the JSON file
        Json json = new Json();
        try {
            return json.fromJson(SaveState.class, file.readString());
        } catch (Exception e) {
            // If the file is corrupted or has invalid data, log the error
            Gdx.app.error("SaveSystem", "Failed to load save", e);
            return null;
        }
    }

    /**
     * Deletes a save file from disk.
     * Used when the player wants to clear a save slot.
     *
     * @param slot the save slot number to delete (1, 2, or 3)
     */
    public static void deleteSave(int slot) {
        FileHandle file = Gdx.files.local("slot" + slot + ".json");
        if (file.exists()) file.delete();
    }
}

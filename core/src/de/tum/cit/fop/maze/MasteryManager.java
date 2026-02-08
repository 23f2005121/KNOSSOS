package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.HashSet;

/**
 * Manages the permanent progression system (skill tree) that persists across game sessions.
 * Players earn XP by defeating enemies and collecting powerups, which converts to skill points.
 * Skill points can be spent to unlock permanent upgrades in four categories:
 * - Speed: Movement speed and stamina improvements
 * - Dash: Dash ability cooldown reduction and invincibility
 * - Attack: Attack speed and damage improvements
 * - Health: Maximum health increases
 * <p>
 * Each category has 4 tiers of upgrades that must be unlocked in order.
 * All progression is automatically saved to disk and persists between game sessions.
 * <p>
 * XP System:
 * - 100 XP = 1 skill point
 * - ChickenJockey: 5 XP
 * - NormalEnemy: 10 XP
 * - BigEnemy: 20 XP
 * - powerups: 5 XP
 */
public class MasteryManager {

    // File where progression data is saved
    private static final String FILE_NAME = "mastery.json";

    // Singleton instance
    private static MasteryManager instance;

    // JSON serializer for saving/loading
    private final Json json;

    /**
     * Container class for all mastery data that gets saved to disk.
     * Includes current XP, available skill points, and all unlocked upgrades.
     */
    public static class MasteryData {
        public int experience = 0;                              // Current XP progress toward next skill point (0-99)
        public int skillPoints = 0;                             // Unspent skill points available
        public HashSet<String> unlockedUpgrades = new HashSet<>(); // Set of all unlocked upgrade keys
    }

    // The current progression data
    private MasteryData data;

    // --- SPEED TREE UPGRADE KEYS ---
    public static final String SPEED_1 = "SPEED_1"; // Tier 1: 1.25x movement speed
    public static final String SPEED_2 = "SPEED_2"; // Tier 2: 2.0x movement speed
    public static final String SPEED_3 = "SPEED_3"; // Tier 3: 2x stamina regeneration rate
    public static final String SPEED_4 = "SPEED_4"; // Tier 4: Unlimited stamina (never depletes)

    // --- DASH TREE UPGRADE KEYS ---
    public static final String DASH_1 = "DASH_1"; // Tier 1: 25% cooldown reduction (0.75x)
    public static final String DASH_2 = "DASH_2"; // Tier 2: 50% cooldown reduction (0.50x)
    public static final String DASH_3 = "DASH_3"; // Tier 3: 75% cooldown reduction (0.25x)
    public static final String DASH_4 = "DASH_4"; // Tier 4: Invincible while dashing

    // --- ATTACK TREE UPGRADE KEYS ---
    public static final String ATTACK_1 = "ATTACK_1"; // Tier 1: Attack speed increase
    public static final String ATTACK_2 = "ATTACK_2"; // Tier 2: Further attack speed increase
    public static final String ATTACK_3 = "ATTACK_3"; // Tier 3: Even faster attacks
    public static final String ATTACK_4 = "ATTACK_4"; // Tier 4: Maximum attack speed

    // --- HEALTH TREE UPGRADE KEYS ---
    public static final String HEALTH_1 = "HEALTH_1"; // Tier 1: +1 max health (4 hearts)
    public static final String HEALTH_2 = "HEALTH_2"; // Tier 2: +1 max health (5 hearts)
    public static final String HEALTH_3 = "HEALTH_3"; // Tier 3: +1 max health (6 hearts)
    public static final String HEALTH_4 = "HEALTH_4"; // Tier 4: +1 max health (7 hearts)

    // --- UPGRADE COSTS IN SKILL POINTS ---
    public static final int COST_1 = 1;   // Cost for tier 1 upgrades
    public static final int COST_2 = 5;   // Cost for tier 2 upgrades
    public static final int COST_3 = 10;  // Cost for tier 3 upgrades
    public static final int COST_4 = 15;  // Cost for tier 4 upgrades

    /**
     * Private constructor that loads saved progression data.
     * Called only once by getInstance() to create the singleton.
     */
    private MasteryManager() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        load(); // Load existing progression from disk
    }

    /**
     * Gets the singleton instance of the MasteryManager.
     * Creates it on first call (lazy initialization).
     *
     * @return the singleton instance
     */
    public static MasteryManager getInstance() {
        if (instance == null) {
            instance = new MasteryManager();
        }
        return instance;
    }

    /**
     * Loads saved progression data from disk.
     * If no save file exists, or it's corrupted, starts with fresh data.
     */
    private void load() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        if (file.exists()) {
            try {
                data = json.fromJson(MasteryData.class, file.readString());
            } catch (Exception e) {
                e.printStackTrace();
                data = new MasteryData(); // Use fresh data if file is corrupted
            }
        } else {
            data = new MasteryData(); // No save file yet, start fresh
        }
        // Ensure the unlocked set is never null
        if (data.unlockedUpgrades == null) data.unlockedUpgrades = new HashSet<>();
    }

    /**
     * Saves the current progression data to disk.
     * Called automatically after any change (earning XP, unlocking upgrades).
     */
    private void save() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        file.writeString(json.prettyPrint(data), false); // false = overwrite
    }

    /**
     * Adds experience points to the player's total.
     * Every 100 XP grants 1 skill point.
     * Can grant multiple skill points if enough XP is added at once.
     *
     * @param amount the amount of XP to add
     * @return true if the player earned at least one skill point, false otherwise
     */
    public boolean addExperience(int amount) {
        int oldPoints = data.skillPoints;
        data.experience += amount;

        boolean leveledUp = false;
        // Convert XP to skill points (100 XP = 1 point)
        while (data.experience >= 100) {
            data.experience -= 100;
            data.skillPoints++;
            leveledUp = true;
        }
        save();
        return leveledUp; // Returns true if a skill point was earned
    }

    /**
     * Attempts to unlock an upgrade in the skill tree.
     * Checks if the player has enough skill points and if prerequisite upgrades are unlocked.
     * Automatically saves on success.
     *
     * @param key         the upgrade key to unlock (e.g., SPEED_1, DASH_2)
     * @param cost        how many skill points this upgrade costs
     * @param previousKey the prerequisite upgrade that must be unlocked first (null for tier 1)
     * @return true if the upgrade was successfully unlocked, false otherwise
     */
    public boolean unlockUpgrade(String key, int cost, String previousKey) {
        // Already unlocked?
        if (data.unlockedUpgrades.contains(key)) return false;

        // Not enough skill points?
        if (data.skillPoints < cost) return false;

        // Prerequisite not unlocked?
        if (previousKey != null && !data.unlockedUpgrades.contains(previousKey)) return false;

        // Check for achievement (all 16 upgrades unlocked)
        if (data.unlockedUpgrades.size() >= 16) {
            AchievementManager.getInstance().unlock("SKILL_GOD");
        }

        // Success! Deduct cost and unlock the upgrade
        data.skillPoints -= cost;
        data.unlockedUpgrades.add(key);
        save();
        return true;
    }

    /**
     * Checks if a specific upgrade has been unlocked.
     *
     * @param key the upgrade key to check (e.g., SPEED_1, DASH_2)
     * @return true if the upgrade is unlocked, false otherwise
     */
    public boolean hasUpgrade(String key) {
        return data.unlockedUpgrades.contains(key);
    }

    /**
     * Gets the number of unspent skill points the player currently has.
     *
     * @return available skill points
     */
    public int getSkillPoints() {
        return data.skillPoints;
    }

    /**
     * Gets the current XP progress toward the next skill point.
     *
     * @return current XP (0-99)
     */
    public int getExperience() {
        return data.experience;
    }
}

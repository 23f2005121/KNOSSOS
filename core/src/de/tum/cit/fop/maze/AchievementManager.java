package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages game achievements, persistence (saving), and the unlock queue.
 * This class follows the Singleton pattern to ensure only one manager exists.
 */
public class AchievementManager {
    // The single instance of this manager (Singleton pattern)
    private static AchievementManager instance;

    // Set of achievement IDs that the player has already unlocked
    private final HashSet<String> unlockedIds = new HashSet<>();

    // List of all possible achievements loaded from the JSON file
    private final ArrayList<AchievementEntry> allAchievements = new ArrayList<>();

    // Queue of achievements waiting to be shown as popups on screen
    private final Queue<AchievementEntry> toastQueue = new LinkedList<>();

    // File name where we save which achievements are unlocked
    private static final String SAVE_FILE = "achievements_unlocked.dat";

    /**
     * Inner class representing a single achievement entry from the JSON file.
     * Contains the ID, display title, and description text.
     */
    public static class AchievementEntry {
        public String id;     // Unique identifier like "first_level"
        public String title;  // Display name like "Baby Steps"
        public String desc;   // Description like "Complete your first level"
    }

    /**
     * Private constructor so nobody can create multiple managers.
     * Loads the achievement definitions and previously unlocked progress.
     */
    private AchievementManager() {
        loadAchievements();
        loadProgress();
    }

    /**
     * Gets the single shared instance of the AchievementManager.
     * Creates it on the first call (lazy initialization).
     *
     * @return The singleton instance of AchievementManager.
     */
    public static AchievementManager getInstance() {
        if (instance == null) instance = new AchievementManager();
        return instance;
    }

    /**
     * Loads the achievement definitions (IDs, Titles, Descriptions) from assets.
     * Reads from the achievements.json file in the assets' folder.
     */
    private void loadAchievements() {
        try {
            Json json = new Json();
            FileHandle file = Gdx.files.internal("achievements.json");

            // Check if the JSON file actually exists
            if (file.exists()) {
                // Parse the JSON into a list of AchievementEntry objects
                ArrayList<AchievementEntry> list = json.fromJson(ArrayList.class, AchievementEntry.class, file);
                if (list != null) allAchievements.addAll(list);
            } else {
                Gdx.app.error("Achievements", "achievements.json NOT FOUND in assets folder!");
            }
        } catch (Exception e) {
            // Log any errors if the JSON is malformed or can't be read
            Gdx.app.error("Achievements", "Error parsing achievements.json", e);
        }
    }

    /**
     * Unlocks an achievement by its ID.
     * If the achievement is already unlocked, nothing happens.
     * Otherwise, it marks it as unlocked, adds it to the toast queue,
     * and saves progress to the file.
     *
     * @param id The unique string ID of the achievement to unlock.
     */
    public void unlock(String id) {
        // Skip if we already unlocked this one
        if (unlockedIds.contains(id)) return;

        // Search for the achievement with the matching ID
        for (AchievementEntry a : allAchievements) {
            if (a.id.equals(id)) {
                unlockedIds.add(id);        // Mark as unlocked
                toastQueue.add(a);          // Queue it for the popup animation
                saveProgress();             // Save to disk immediately
                Gdx.app.log("Achievement", "Unlocked: " + a.title);
                break;
            }
        }
    }

    /**
     * Retrieves the next achievement waiting to be shown on screen.
     * Removes it from the queue and returns it.
     *
     * @return The next AchievementEntry, or null if the queue is empty.
     */
    public AchievementEntry getNextInQueue() {
        return toastQueue.poll(); // poll() removes and returns the head, or null if empty
    }

    /**
     * Clears all progress and deletes the local save file.
     * Useful for testing or if the player wants to reset achievements.
     */
    public void resetAchievements() {
        unlockedIds.clear();
        FileHandle file = Gdx.files.local(SAVE_FILE);
        if (file.exists()) file.delete();
        Gdx.app.log("Achievement", "All progress has been reset.");
    }

    /**
     * Saves the IDs of unlocked achievements to a local file.
     * Each ID is written on a separate line.
     */
    private void saveProgress() {
        FileHandle file = Gdx.files.local(SAVE_FILE);
        StringBuilder sb = new StringBuilder();
        // Write each unlocked ID on its own line
        for (String id : unlockedIds) sb.append(id).append("\n");
        file.writeString(sb.toString(), false);
    }

    /**
     * Loads previously unlocked achievements from the local file.
     * Called when the game starts so progress persists between sessions.
     */
    private void loadProgress() {
        FileHandle file = Gdx.files.local(SAVE_FILE);
        if (file.exists()) {
            // Read the file and split by newlines
            String[] lines = file.readString().split("\n");
            for (String s : lines) if (!s.isEmpty()) unlockedIds.add(s);
        }
    }

    /**
     * Counts how many achievements the player has unlocked so far.
     *
     * @return The total number of achievements currently unlocked.
     */
    public int getUnlockedCount() {
        return unlockedIds.size();
    }

    /**
     * Returns the full list of achievements defined in the JSON,
     * regardless of whether they're unlocked or not.
     *
     * @return A list of every possible achievement defined in the JSON.
     */
    public ArrayList<AchievementEntry> getAllAchievements() {
        return allAchievements;
    }

    /**
     * Checks if a specific achievement has been earned by the player.
     *
     * @param id The ID to check.
     * @return true if unlocked, false otherwise.
     */
    public boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }
}

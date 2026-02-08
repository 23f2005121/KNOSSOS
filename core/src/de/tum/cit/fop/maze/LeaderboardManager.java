package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Manages the game's high score leaderboard for endless mode.
 * Keeps track of the top 10 scores, saves them to disk, and loads them on startup.
 * Uses the Singleton pattern to ensure only one leaderboard exists.
 * <p>
 * Each entry includes the final score, number of levels cleared, and the date achieved.
 * Scores are automatically sorted in descending order (highest first).
 */
public class LeaderboardManager {

    // The single instance of the leaderboard manager (Singleton pattern)
    private static LeaderboardManager instance;

    // File name where high scores are saved on disk
    private static final String FILE_NAME = "leaderboard.json";

    // List of the top 10 high scores, sorted highest to lowest
    private final ArrayList<ScoreEntry> highScores = new ArrayList<>();

    // JSON serializer for reading and writing scores to file
    private final Json json = new Json();

    /**
     * Represents a single entry on the leaderboard.
     * Contains the final score, date achieved, and how many levels were cleared.
     */
    public static class ScoreEntry {
        public int score;          // Total score achieved in the run
        public String date;        // Date when this score was achieved (abbreviated)
        public int levelsCleared;  // Number of levels completed before dying
    }

    /**
     * Private constructor that loads saved high scores from disk.
     * Called only once by getInstance() to create the singleton.
     */
    private LeaderboardManager() {
        load(); // Load existing scores from file
    }

    /**
     * Gets the single shared instance of the LeaderboardManager.
     * Creates it on first call (lazy initialization).
     *
     * @return the singleton instance of LeaderboardManager
     */
    public static LeaderboardManager getInstance() {
        if (instance == null) instance = new LeaderboardManager();
        return instance;
    }

    /**
     * Adds a new score to the leaderboard if it qualifies for the top 10.
     * Automatically sorts the list and removes the lowest score if more than 10 entries exist.
     * Saves the updated leaderboard to disk immediately.
     *
     * @param score  the total score achieved in this run
     * @param levels the number of levels completed before game over
     */
    public void addScore(int score, int levels) {
        // Create a new score entry
        ScoreEntry entry = new ScoreEntry();
        entry.score = score;
        entry.levelsCleared = levels;

        // Create a simple date string (e.g., "Jan 30")
        entry.date = new java.util.Date().toString().substring(4, 10);

        // Add the new entry to the list
        highScores.add(entry);

        // Sort the list in descending order (highest score first)
        Collections.sort(highScores, (a, b) -> Integer.compare(b.score, a.score));

        // Keep only the top 10 scores
        if (highScores.size() > 10) highScores.remove(highScores.size() - 1);

        // Save the updated leaderboard to disk
        save();
    }

    /**
     * Writes the current high scores to disk as a JSON file.
     * Called automatically after adding a new score.
     */
    private void save() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        file.writeString(json.prettyPrint(highScores), false); // false means overwrite
    }

    /**
     * Loads previously saved high scores from disk.
     * Called when the LeaderboardManager is first created.
     * If no file exists, the leaderboard starts empty.
     */
    private void load() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        if (file.exists()) {
            // Parse the JSON file into a list of ScoreEntry objects
            ArrayList<ScoreEntry> loaded = json.fromJson(ArrayList.class, ScoreEntry.class, file);
            if (loaded != null) highScores.addAll(loaded);
        }
    }

    /**
     * Returns the list of high scores for display on the leaderboard screen.
     * The list is already sorted from highest to lowest score.
     *
     * @return the list of top 10 high scores
     */
    public ArrayList<ScoreEntry> getHighScores() {
        return highScores;
    }
}

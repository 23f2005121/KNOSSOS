package de.tum.cit.fop.maze;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the scoring system for both story and endless modes.
 * Tracks player performance through a time-decay base score and bonus/penalty system.

 */
public class ScoreManager {
    // Singleton instance
    private static ScoreManager instance;

    // Cumulative score across all completed levels (story or endless session)
    private int totalGameScore = 0;

    // Current level's base score (decays over time)
    private float currentLevelBaseScore = 5000f;

    // How many points decay per second (10 points/second)
    private final float DECAY_RATE = 10f;

    // Detailed breakdown of score components for victory screen display
    // LinkedHashMap preserves insertion order for consistent UI layout
    private final Map<String, Integer> breakdown = new LinkedHashMap<>();

    // Bonuses and penalties accumulated during the current level
    private int sessionBonus = 0;

    /**
     * Private constructor to enforce singleton pattern.
     * Use getInstance() to access the manager.
     */
    private ScoreManager() {}

    /**
     * Gets the singleton instance of the ScoreManager.
     * Creates a new instance if one doesn't exist yet.
     *
     * @return the global ScoreManager instance
     */
    public static ScoreManager getInstance() {
        if (instance == null)
            instance = new ScoreManager();
        return instance;
    }

    /**
     * Updates the base score decay every frame.
     * Called in GameScreen's update loop while the level is active.
     *
     * @param delta time elapsed since last frame in seconds (~0.016s at 60 FPS)
     */
    public void update(float delta) {
        // Decay base score over time, but never below 500 minimum
        if (currentLevelBaseScore > 500) {
            currentLevelBaseScore -= DECAY_RATE * delta;
        }
    }

    /**
     * Adds bonus points for positive player actions.
     * Used for achievements like collecting keys, killing enemies, finding secrets, etc.
     *
     * @param label descriptive name for the bonus (shown on victory screen)
     * @param points number of points to award (usually 100-1000)
     */
    public void addBonus(String label, int points) {
        sessionBonus += points;
        breakdown.put(label, breakdown.getOrDefault(label, 0) + points);
    }

    /**
     * Subtracts penalty points for negative events.
     * Used for mistakes like taking damage or dying.
     *
     * @param label descriptive name for the penalty (shown on victory screen)
     * @param points number of points to subtract (positive value, gets subtracted)
     */
    public void subtractPenalty(String label, int points) {
        sessionBonus -= points;
        breakdown.put(label, breakdown.getOrDefault(label, 0) - points);
    }

    /**
     * Finalizes the level score and adds completion bonuses.
     * Called when the player successfully completes a level (reaches the exit).
     *
     * @param remainingLives player's health at level completion (for health bonus)
     * @param levelTimer     time taken to complete the level in seconds (for speedrun check)
     */
    public void finishLevel(int remainingLives, float levelTimer) {
        // Record the decayed base score as "Time Performance"
        breakdown.put("Time Performance", (int)currentLevelBaseScore);

        // Award health bonus: 100 points per remaining life
        addBonus("Lives Remaining", remainingLives * 100);

        // Award speedrun bonus if completed in under 30 seconds
        if (levelTimer < 30) {
            addBonus("Speedrun Bonus", 1000);
        }

        // Add this level's total to the cumulative game score
        totalGameScore += getLevelTotal();
    }

    /**
     * Resets score state for the start of a new level.
     * Called when transitioning to the next level in story mode or endless mode.
     * <p>
     * Called by:
     * - Story mode: When advancing to next story level
     * - Endless mode: When generating next procedural level
     */
    public void startNewLevel() {
        currentLevelBaseScore = 5000f; // Reset to starting base score
        sessionBonus = 0;              // Clear accumulated bonuses
        breakdown.clear();             // Clear score breakdown
    }

    /**
     * Resets the total game score for a new endless mode session.
     * Called when starting a fresh endless mode run.
     * <p>
     * This is different from startNewLevel() because it resets the cumulative score.
     * Used when:
     * - Starting a new endless mode session from the menu
     * - Player wants to start a fresh endless run after game over
     * <p>
     * Story mode does NOT call this - story scores accumulate across the entire campaign.
     */
    public void resetTotalScore() {
        this.totalGameScore = 0; // Reset cumulative score
        startNewLevel();         // Also reset current level score
    }

    /**
     * Finalizes the score when the player dies (game over).
     * Adds whatever score was earned in the current level to the total game score.
     * <p>
     * Called by GameScreen when player health reaches 0.
     */
    public void finalizeLoss() {
        totalGameScore += getLevelTotal();
    }

    /**
     * Calculates the total score for the current level.
     * Combines the time-decayed base score with all accumulated bonuses and penalties.
     * <p>
     * Formula: Level Total = Current Base Score + Session Bonus
     *
     * @return total points earned in the current level
     */
    public int getLevelTotal() {
        return (int)currentLevelBaseScore + sessionBonus;
    }

    /**
     * Gets the cumulative score across all completed levels.
     * This is the final score displayed at game over or story completion.
     * <p>
     * Story Mode: Sum of all level scores through the campaign
     * Endless Mode: Sum of all level scores in the current endless session
     *
     * @return total game score across all levels
     */
    public int getTotalGameScore() {
        return totalGameScore;
    }

    /**
     * Gets the detailed score breakdown for display on the victory screen.
     * Returns a map of label → points for all score components.
     *
     *
     * @return map of score component labels to point values
     */
    public Map<String, Integer> getBreakdown() {
        return breakdown;
    }

    /**
     * Gets the current base score (time performance).
     * This is the 5000 starting score after time decay has been applied.
     * Used for displaying live score during gameplay.
     *
     * @return current base score (500-5000 range)
     */
    public int getCurrentBase() {
        return (int)currentLevelBaseScore;
    }

    /**
     * Loads saved score state from a save file.
     * Used by GameStateManager when loading story mode progress.

     * Called when loading a story mode save file.
     *
     * @param totalScore            cumulative score to restore
     * @param currentLevelBaseScore base score of current level to restore
     * @param sessionBonus          accumulated bonuses/penalties to restore
     */
    public void loadScoreState(int totalScore, float currentLevelBaseScore, int sessionBonus) {
        this.totalGameScore = totalScore;
        this.currentLevelBaseScore = currentLevelBaseScore;
        this.sessionBonus = sessionBonus;
    }

    /**
     * Gets the current level's base score as a float (preserves decimal precision).
     * Used by save system to preserve exact decay state.
     *
     * @return current base score with decimal precision
     */
    public float getLevelBaseScore() {
        return currentLevelBaseScore;
    }

    /**
     * Gets the current session's accumulated bonuses and penalties.
     * This is the sum of all addBonus() and subtractPenalty() calls.
     * Used by save system and for live score display.
     *
     * @return accumulated bonus/penalty points (can be negative)
     */
    public int getSessionBonus() {
        return sessionBonus;
    }
}

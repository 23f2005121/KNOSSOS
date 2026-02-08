package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The class VoiceManager acts as an audio manager for all vocal sound effects in the game.
 * some of the features include category-based volume control, and global mute functionality.
 *
 */
public final class VoiceManager {

    /* ===================== ENUMS ===================== */

    /**
     * Sound categories for volume control and organization.
     * Each category represents a different audio "layer" in the game's soundscape.
     *
     */
    public enum Category {
        PLAYER,
        ENEMY,
        UI,
        ANNOUNCER
    }

    /* ===================== DATA ===================== */

    // Master registry: sound key → loaded Sound object
    // Example: "player_attack_1" → Sound instance
    private static final Map<String, Sound> sounds = new HashMap<>();

    // Cooldown tracker: sound key → last play timestamp (milliseconds)
    // Prevents rapid-fire spam of the same sound
    private static final Map<String, Long> lastPlayed = new HashMap<>();

    // Volume multiplier per category (0.0 to 1.0)
    // Applied on top of the per-sound volume parameter
    private static final Map<Category, Float> categoryVolume = new EnumMap<>(Category.class);

    // Minimum time between identical sound plays (milliseconds)
    // 150ms = ~6.67 sounds per second max (prevents overlap spam)
    private static final long DEFAULT_COOLDOWN = 150;

    // Initialization guard to prevent double-loading
    private static boolean initialized = false;


    // Global volume multiplier (0.0 to 1.0)
    // Controlled by the "SFX Volume" slider in options
    private static float globalVolume = 0.5f;



    /* ===================== INIT ===================== */

    /**
     * Initializes the VoiceManager and loads all sound files.
     * Must be called ONCE at game startup (typically in MazeRunnerGame.create()).
     *
     */
    public static void init() {
        // Prevent double initialization
        if (initialized)
            return;

        // ===== SET DEFAULT CATEGORY VOLUMES =====
        // These act as multipliers on the per-sound volume
        categoryVolume.put(Category.PLAYER, 1.0f);    // 100% (full volume)
        categoryVolume.put(Category.ENEMY, 1.0f);     // 100%
        categoryVolume.put(Category.UI, 0.8f);        // 80% (slightly quieter for comfort)
        categoryVolume.put(Category.ANNOUNCER, 1.0f); // 100% (important events)

        // player sounds
        // Combat sounds
        load("player_attack_1", "sounds/voice/player/player_attack_1.wav");
        load("player_hurt_1", "sounds/voice/player/player_hurt_1.wav");
        load("player_hurt_2", "sounds/voice/player/player_hurt_2.wav");
        load("player_death", "sounds/voice/player/player_death.wav");

        // Movement sounds
        load("player_step_1", "sounds/voice/player/step_1.wav");
        load("player_step_2", "sounds/voice/player/step_2.wav");
        load("player_dash", "sounds/voice/player/dash.wav");
        load("player_sprint", "sounds/voice/player/sprint.wav");

        /* ===================== ENEMY SOUNDS ===================== */
        // Death sounds (one per enemy type)
        load("enemy_normal_die", "sounds/voice/enemy/normal_die.wav");    // Skeleton
        load("enemy_big_die", "sounds/voice/enemy/big_die.wav");          // Slime
        load("enemy_mage_die", "sounds/voice/enemy/mage_die.wav");        // Mage
        load("enemy_chicken_die", "sounds/voice/enemy/chicken_die.wav");  // Chicken Jockey

        // Hit/attack sounds
        load("enemy_snail_hit", "sounds/voice/enemy/snail_hit.wav");      // Immortal Snail
        load("enemy_hit", "sounds/voice/enemy/hit.wav");                  // Generic hit
        load("enemy_mage_shoot", "sounds/voice/enemy/mage_shoot.wav");    // Mage fireball

        // ui sounds
        // Button interactions
        load("ui_click", "sounds/voice/ui/ui_click.wav");
        load("ui_toggle_on", "sounds/voice/ui/toggle_on.wav");
        load("ui_toggle_off", "sounds/voice/ui/toggle_off.wav");
        load("ui_hover", "sounds/voice/ui/hover.wav");
        load("ui_back", "sounds/voice/ui/back.wav");
        load("ui_error", "sounds/voice/ui/error.wav");

        // collectible sounds
        // Pickup sounds for various collectible types
        load("collect_heart", "sounds/voice/collect/heart.wav");
        load("collect_key", "sounds/voice/collect/key.wav");
        load("collect_powerup", "sounds/voice/collect/powerup.wav");
        load("collect_weapon", "sounds/voice/collect/weapon.wav");
        load("collect_coin", "sounds/voice/collect/coin.wav");

        // trap sounds
        // Trap activation sounds
        load("trap_spike", "sounds/voice/trap/spike.wav");
        load("trap_slow", "sounds/voice/trap/slow.wav");
        load("trap_root", "sounds/voice/trap/root.wav");

        // level event sounds
        // Level transition and progression sounds
        load("level_start", "sounds/voice/level/start.wav");
        load("level_complete", "sounds/voice/level/complete.wav");
        load("door_open", "sounds/voice/level/door_open.wav");

        // announcer sounds
        // Major game events (highest priority audio)
        load("achievement_unlocked", "sounds/voice/announcer/achievement.wav");
        load("game_over", "sounds/voice/announcer/game_over.wav");
        load("victory", "sounds/voice/announcer/victory.wav");

        initialized = true;
    }

    // loading method

    /**
     * Loads a single sound file and registers it with the manager.
     * Called during initialization for each sound in the library.
     *
     * @param key  unique identifier for this sound (e.g., "player_attack_1")
     * @param path file path relative to assets directory
     */
    private static void load(String key, String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                sounds.put(key, Gdx.audio.newSound(Gdx.files.internal(path)));
            } else {
                Gdx.app.error("VoiceManager", "Sound file not found: " + path);
            }
        } catch (Exception e) {
            Gdx.app.error("VoiceManager", "Failed to load sound: " + path, e);
        }
    }

    // core playing method

    /**
     * Core sound playing method with full control over all parameters.
     * All convenience methods (playPlayer, playUI, etc.) delegate to this.
     *
     *
     * @param key      sound identifier (must be loaded via init())
     * @param category which category this sound belongs to
     * @param pitchMin minimum pitch multiplier (0.5 = half speed, 2.0 = double speed)
     * @param pitchMax maximum pitch multiplier
     * @param volume   base volume (0.0 to 1.0, before category multiplier)
     */
    public static void play(
            String key,
            Category category,
            float pitchMin,
            float pitchMax,
            float volume) {

        // Check global mute toggle (from options menu)
        if (!GamePreferences.voiceEnabled) {
            return;
        }

        // Retrieve sound from registry
        Sound sound = sounds.get(key);
        if (sound == null) {
            Gdx.app.error("VoiceManager", "Sound not found: " + key);
            return;
        }

        // Check cooldown to prevent spam
        if (isOnCooldown(key)) {
            return;
        }

        // Randomize pitch for audio variety
        float pitch = MathUtils.random(pitchMin, pitchMax);

        // Calculate final volume (base × category multiplier × global volume)
        float finalVolume = volume * categoryVolume.getOrDefault(category, 1.0f) * globalVolume;

        // Play the sound (volume, pitch, pan)
        // Pan = 0.0 means centered (no stereo panning)
        sound.play(finalVolume, pitch, 0f);

        // Record timestamp for cooldown tracking
        lastPlayed.put(key, Time.now());
    }

    // helper methods
    /**
     * Plays a player-related sound with subtle pitch variation.
     * Used for: attacks, hurt sounds, footsteps, dash, sprint.
     * <p>
     * Pitch range: 0.95 to 1.05 (±5%)
     * Category: PLAYER (100% volume by default)
     *
     * @param key sound identifier (e.g., "player_attack_1")
     */
    public static void playPlayer(String key) {
        play(key, Category.PLAYER, 0.95f, 1.05f, 1.0f);
    }

    /**
     * Plays an enemy-related sound with moderate pitch variation.
     * Used for: enemy deaths, hits, special attacks.
     * <p>
     * Pitch range: 0.9 to 1.1 (±10%)
     * Category: ENEMY (100% volume by default)
     *
     * @param key sound identifier (e.g., "enemy_normal_die")
     */
    public static void playEnemy(String key) {
        play(key, Category.ENEMY, 0.9f, 1.1f, 1.0f);
    }

    /**
     * Plays a UI sound with no pitch variation (consistency).
     * Used for: button clicks, toggles, navigation, errors.
     * <p>
     * Pitch range: 1.0 to 1.0 (no variation)
     * Category: UI (80% volume by default, slightly quieter)
     *
     * @param key sound identifier (e.g., "ui_click")
     */
    public static void playUI(String key) {
        play(key, Category.UI, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Plays an announcer sound with no pitch variation (clarity).
     * Used for: achievements, game over, victory.
     * <p>
     * Pitch range: 1.0 to 1.0 (no variation)
     * Category: ANNOUNCER (100% volume, important events)
     *
     * @param key sound identifier (e.g., "achievement_unlocked")
     */
    public static void playAnnouncer(String key) {
        play(key, Category.ANNOUNCER, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Plays a collectible pickup sound with slight pitch variation.
     * Used for: hearts, keys, powerups, weapons, coins.
     * <p>
     * Pitch range: 0.95 to 1.05 (±5%)
     * Category: UI (80% volume, treated as UI feedback)
     * Volume: 0.9 (slightly quieter than UI buttons)
     *
     * @param key sound identifier (e.g., "collect_heart")
     */
    public static void playCollect(String key) {
        play(key, Category.UI, 0.95f, 1.05f, 0.9f);
    }

    /**
     * Plays a trap/hazard activation sound with moderate pitch variation.
     * Used for: spike traps, slowness traps, root traps.
     * <p>
     * Pitch range: 0.9 to 1.1 (±10%)
     * Category: ENEMY (treated as environmental hazard)
     * Volume: 0.8 (quieter than direct enemy sounds)
     *
     * @param key sound identifier (e.g., "trap_spike")
     */
    public static void playTrap(String key) {
        play(key, Category.ENEMY, 0.9f, 1.1f, 0.8f);
    }

    /**
     * Plays a level event sound with no pitch variation (consistency).
     * Used for: level start, level complete, door opening.
     * <p>
     * Pitch range: 1.0 to 1.0 (no variation)
     * Category: ANNOUNCER (important progression events)
     * Volume: 0.9 (slightly quieter than victory/game over)
     *
     * @param key sound identifier (e.g., "level_start")
     */
    public static void playLevel(String key) {
        play(key, Category.ANNOUNCER, 1.0f, 1.0f, 0.9f);
    }

    // random variants

    /**
     * Plays a random sound from a list of variants.
     * Increases audio variety by randomly selecting from multiple similar sounds.
     * <p>
     *
     * @param category which category these sounds belong to
     * @param keys     array of sound identifiers to choose from
     */
    public static void playRandom(Category category, String... keys) {
        if (keys == null || keys.length == 0)
            return;

        // Pick random key from array
        String key = keys[MathUtils.random(keys.length - 1)];

        // Play with standard pitch variation
        play(key, category, 0.95f, 1.05f, 1.0f);
    }

    // cooldown

    /**
     * Checks if a sound is on cooldown (played too recently).
     * Prevents audio spam by enforcing a minimum time between identical sounds.
     *
     * @param key sound identifier to check
     * @return true if sound is on cooldown, false if ready to play
     */
    private static boolean isOnCooldown(String key) {
        long now = Time.now();
        long last = lastPlayed.getOrDefault(key, 0L);
        return now - last < DEFAULT_COOLDOWN;
    }

    // volume

    /**
     * Sets the volume multiplier for an entire category.
     * Affects all sounds in that category globally.
     *
     * @param category which category to adjust
     * @param volume   new volume multiplier (0.0 to 1.0)
     */
    public static void setCategoryVolume(Category category, float volume) {
        categoryVolume.put(category, MathUtils.clamp(volume, 0f, 1f));
    }

    // dispose methods

    /**
     * Cleans up all loaded sound resources.
     * Must be called in MazeRunnerGame.dispose() to prevent memory leaks.
     * <p>
     *
     * After calling dispose(), init() must be called again before using sounds.
     */
    public static void dispose() {
        // Dispose all Sound objects (releases native audio resources)
        for (Sound s : sounds.values()) {
            s.dispose();
        }

        // Clear all data structures
        sounds.clear();
        lastPlayed.clear();

        // Allow re-initialization
        initialized = false;
    }

    // time helper method

    /**
     * Simple time utility for cooldown tracking.
     * Uses system time (milliseconds since epoch) for high-precision timestamps.
     * <p>
     * Nested class keeps time logic encapsulated and out of the main API.
     */
    private static final class Time {
        /**
         * Gets current system time in milliseconds.
         * Used for cooldown timestamp comparisons.
         *
         * @return current time in milliseconds since epoch
         */
        static long now() {
            return System.currentTimeMillis();
        }
    }

    public static void setVolume(float vol) {
        globalVolume = MathUtils.clamp(vol, 0f, 1f);
    }

    public static float getVolume() {
        return globalVolume;
    }


}

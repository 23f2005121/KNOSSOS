package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Manages the persistent game settings such as music volume and key bindings.
 * This class wraps LibGDX's {@link Preferences} system and provides
 * typed getter and setter methods for all configurable options.
 * All changes are immediately saved using {@code prefs.flush()}.
 */
public class GamePreferences {

    private static final String PREFS_FILE = "keybindings.json";
    private final Json json;
    private InputSettings settings;

    // Global toggle (kept static for easy access, but synced with JSON)
    public static boolean voiceEnabled = true;



    public static class InputSettings {
        public float musicVolume = 0.5f;
        public float soundVolume = 0.5f;
        public boolean voiceEnabled = true;

        // Movement
        public int keyUp = Input.Keys.UP;
        public int keyDown = Input.Keys.DOWN;
        public int keyLeft = Input.Keys.LEFT;
        public int keyRight = Input.Keys.RIGHT;

        // Actions
        public int keyDash = Input.Keys.CONTROL_LEFT;
        public int keyAttack = Input.Keys.SPACE;
        public int keySprint = Input.Keys.SHIFT_LEFT;

        // View / System
        public int keyZoomIn = Input.Keys.I;
        public int keyZoomOut = Input.Keys.O;
        public int keyFullscreen = Input.Keys.F;
        public int keyConsole = Input.Keys.T;
    }

    /**
     * Creates a new GamePreferences instance and loads saved settings from disk.
     * If no settings file exists yet, it will be created with default values.
     * Loads the sound effects toggle state so it persists between sessions.
     */
    public GamePreferences() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        load();
    }



    /**
     * Loads settings from the local JSON file.
     * Uses defaults if file does not exist.
     */
    private void load() {
        FileHandle file = Gdx.files.local(PREFS_FILE);

        if (file.exists()) {
            try {
                settings = json.fromJson(InputSettings.class, file);
            } catch (Exception e) {
                Gdx.app.error("GamePreferences", "Error reading json, using defaults", e);
                settings = new InputSettings();
            }
        } else {
            settings = new InputSettings();
        }

        // Sync the static variable
        voiceEnabled = settings.voiceEnabled;

        VoiceManager.setVolume(settings.soundVolume);
    }

    /**
     * Writes current settings to the local JSON file.
     */
    private void save() {
        // Sync the static variable before saving
        settings.voiceEnabled = voiceEnabled;

        FileHandle file = Gdx.files.local(PREFS_FILE);
        file.writeString(json.prettyPrint(settings), false);
    }




    // volume Settings

    /**
     * Gets the saved background music volume level.
     *
     * @return volume as a float between 0.0 (silent) and 1.0 (full volume), defaults to 0.5
     */
    public float getMusicVolume() {
        return settings.musicVolume;
    }

    /**
     * Sets and saves the background music volume.
     * Change takes effect immediately and is saved to disk.
     *
     * @param volume the new volume level (typically 0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        settings.musicVolume = volume;
        save();
    }

    // --- SFX Volume Settings ---

    public float getSoundVolume() {
        return settings.soundVolume;
    }

    public void setSoundVolume(float volume) {
        settings.soundVolume = volume;
        save();
    }

    // --- Movement Key Bindings ---

    public int getKeyUp() {
        return settings.keyUp;
    }

    public void setKeyUp(int key) {
        settings.keyUp = key;
        save();
    }

    public int getKeyDown() {
        return settings.keyDown;
    }

    public void setKeyDown(int key) {
        settings.keyDown = key;
        save();
    }

    public int getKeyLeft() {
        return settings.keyLeft;
    }

    public void setKeyLeft(int key) {
        settings.keyLeft = key;
        save();
    }

    public int getKeyRight() {
        return settings.keyRight;
    }

    public void setKeyRight(int key) {
        settings.keyRight = key;
        save();
    }

    // --- Action Key Bindings ---

    public int getKeyDash() {
        return settings.keyDash;
    }

    public void setKeyDash(int key) {
        settings.keyDash = key;
        save();
    }

    public int getKeyAttack() {
        return settings.keyAttack;
    }

    public void setKeyAttack(int key) {
        settings.keyAttack = key;
        save();
    }

    public int getKeySprint() {
        return settings.keySprint;
    }

    public void setKeySprint(int key) {
        settings.keySprint = key;
        save();
    }

    // --- View Control Key Bindings ---

    public int getKeyZoomIn() {
        return settings.keyZoomIn;
    }

    public void setKeyZoomIn(int key) {
        settings.keyZoomIn = key;
        save();
    }

    public int getKeyZoomOut() {
        return settings.keyZoomOut;
    }

    public void setKeyZoomOut(int key) {
        settings.keyZoomOut = key;
        save();
    }

    public int getKeyFullscreen() {
        return settings.keyFullscreen;
    }

    public void setKeyFullscreen(int key) {
        settings.keyFullscreen = key;
        save();
    }

    public int getKeyConsole() {
        return settings.keyConsole;
    }

    public void setKeyConsole(int key) {
        settings.keyConsole = key;
        save();
    }

    public boolean isVoiceEnabled() {
        return settings.voiceEnabled;
    }

    public void setVoiceEnabled(boolean enabled) {
        // Update both the static reference and the JSON object
        voiceEnabled = enabled;
        settings.voiceEnabled = enabled;
        save();
    }


}

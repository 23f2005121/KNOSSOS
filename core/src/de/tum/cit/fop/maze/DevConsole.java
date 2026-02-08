package de.tum.cit.fop.maze;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.Gdx;
import java.util.HashMap;

/**
 * Developer console for entering cheat commands during gameplay.
 * Opened with a hotkey (usually tilde or F1) and executes various debug commands.
 * Uses a HashMap to map command strings to their corresponding actions.
 */
public class DevConsole {
    // LibGDX Stage that holds the text input field
    private final Stage stage;

    // The text field where the player types commands
    private final TextField inputField;

    // Maps command strings (like "/ghost") to the code that should run
    private final HashMap<String, Runnable> commands;

    // Tracks whether the console is currently visible and accepting input
    private boolean visible = false;

    /**
     * Creates the developer console and registers all available commands.
     *
     * @param skin       the UI skin for styling the text field
     * @param gameScreen reference to the game screen for level/enemy manipulation
     * @param player     reference to the player for stat manipulation
     * @param map        reference to the map manager (reserved for future commands)
     */
    public DevConsole(Skin skin, GameScreen gameScreen, Player player, MapManager map) {
        this.stage = new Stage();
        this.commands = new HashMap<>();

        // --- DEFINE COMMANDS ---
        // Each command is stored as a string key mapped to a Runnable (code to execute)

        // Toggle ghost mode (walk through walls)
        commands.put("/ghost", () -> player.setGhostMode(!player.isGhostMode()));

        // Toggle immortality (can't take damage)
        commands.put("/immortal", () -> player.setImmortal(!player.isImmortal()));

        // Give a very long speed boost (9999 seconds, basically permanent)
        commands.put("/speed", () -> player.activateSpeedBoost(9999f));

        // Add one extra life
        commands.put("/addhealth", () -> player.addLives(1));

        // Instantly obtain the key without finding it
        commands.put("/getkey", () -> gameScreen.cheatGetKey());

        // Skip to the next level immediately
        commands.put("/nextlevel", () -> gameScreen.startNewLevel());

        // Kill all enemies currently on the map
        commands.put("/killall", () -> gameScreen.cheatKillEnemies());

        // Clear all active cheats and buffs (reset to normal state)
        commands.put("/clear", () -> {
            player.setGhostMode(false);
            player.setImmortal(false);
            player.activateSpeedBoost(0);
        });

        // Give experience points for testing the progression system
        commands.put("/giveexp", () -> {
            MasteryManager.getInstance().addExperience(99);
            System.out.println("Gave 2000 exp (20 SP)");
        });

        // --- UI SETUP ---
        // Create the text field where commands are typed
        inputField = new TextField("", skin);
        inputField.setMessageText("Enter command..."); // Placeholder text

        // Position the text field at the bottom left of the screen
        Table table = new Table();
        table.bottom().left();
        table.setFillParent(true);
        table.add(inputField).width(400).pad(10);

        stage.addActor(table);

        // Listen for the Enter key to execute the command
        inputField.setTextFieldListener((textField, c) -> {
            // Check if Enter was pressed
            if (c == '\r' || c == '\n') {
                handleInput(textField.getText()); // Process the command
                textField.setText(""); // Clear the input field

                // Close the console and return control to the player
                this.setVisible(false);
                Gdx.input.setInputProcessor(gameScreen.getGameInputProcessor());
            }
        });
    }

    /**
     * Processes the entered command and executes it if it exists.
     * Commands are case-insensitive and whitespace is trimmed.
     *
     * @param text the raw text entered by the user
     */
    private void handleInput(String text) {
        // Normalize the input to lowercase and remove extra spaces
        String cmd = text.trim().toLowerCase();

        // Check if the command exists in our map
        if (commands.containsKey(cmd)) {
            commands.get(cmd).run(); // Execute the code associated with this command
            System.out.println("Console: Executed " + cmd);

            // Unlock the achievement for using cheats
            AchievementManager.getInstance().unlock("CHEAT_USED");
        }
    }

    /**
     * Shows or hides the console.
     * When shown, gives keyboard focus to the input field.
     *
     * @param visible true to show the console, false to hide it
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        // When opening the console, focus the text field so typing works immediately
        if (visible) stage.setKeyboardFocus(inputField);
    }


    public boolean isVisible() { return visible; }

    public Stage getStage() { return stage; }
}

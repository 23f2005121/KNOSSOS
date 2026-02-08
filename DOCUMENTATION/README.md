# **KNOSSOS**

#### by TUMato

* Nihar
* Waleed
* Agrim
* Azhaan





## **1. The Class Hierarchy (Game Objects)**



All visible entities in the game world inherit from a common class. This allows the rendering loop to treat them polymorphically.

    GameObject (Base class: Position, Bounds, Texture, Rendering)
        Player (Handles input, movement, stamina, health, mastery logic)
        Item (Base for pickups)
            CollectableLives (Hearts)
            CollectablePowerUps (Speed, Shield, Damage Boosts)
        Weapons (Permanent damage upgrades for the level)
        MageBolt (Projectile fired by the MageEnemy)
        Obstacle (Base for hazardous objects)
            Traps (Static hazards: Spikes, Slime, Vines)
            Enemy (Base AI logic, pathfinding, health)
                NormalEnemy (Skeleton)
                BigEnemy (Slime - tanky, splits logic)
                ChickenJockey (Bat - fast, flying)
                MageEnemy (Stationary, ranged attacks)
                ImmortalSnail (Ghost - Invincible, slow chasing mechanic, can phase through walls and instantly kills player on contact)





## **2. Game Architecture \& Screens**



The application logic follows the standard LibGDX Game and Screen pattern.



    MazeRunnerGame (extends Game): The entry point
        Manages shared resources: SpriteBatch, Skin, GamePreferences, and Audio.
        Handles global asset loading (textures, animations).
        Manages navigation between screens.
    Screens (implements Screen):
        IntroScreen: Plays the frame-by-frame startup animation.
        MenuScreen: Main entry for navigation (Play, Options, Credits, etc.).
        SaveSelectionScreen: Handles the 4 save slots for Story Mode.
        GameScreen: The core gameplay loop.
            Manages the MapManager, Player, Hud, and Entity Lists (obstacles, hearts, bolts).
            Handles the Update/Render cycle for the actual game.
        VictoryScreen / GameOverScreen: Post-game state displays.
        OptionsMenu: Configures volume and key bindings.
        MasteryScreen: The skill tree UI for spending XP.
        LeaderboardScreen: Displays top scores for Endless Mode.
        AchievementScreen: Lists unlocked achievements.





## **3. Managers \& Systems (Logic Separation)**



The game separates specific logic into dedicated managers, many of which use the Singleton pattern.

    Map & World Generation:
     	MapManager: Orchestrates rendering, tile autotiling (bitmasking), and collision detection.
     	StoryLevelLoader: Parses .properties files for fixed Story Mode levels.
     	MazeMap: Uses Prim's Algorithm to generate procedural grids for Endless Mode.
     	AI: Implements A\* Pathfinding logic used by enemies.

    Progression & Data:
     	GameStateManager: Handles JSON serialization for saving/loading Story Mode state (entities, health, position).
            ScoreManager (Singleton): Tracks live score, time decay, and victory breakdown.
         	LeaderboardManager (Singleton): Manages top 10 high scores for Endless Mode.
            MasteryManager (Singleton): Handles the XP system, skill points, and permanent upgrades (Speed, Dash, Attack, Health trees).
            AchievementManager (Singleton): Tracks and saves achievement unlocks.
         	GamePreferences: Handles persistent settings (Keybindings, Volume).

    Audio & Feedback:
     	VoiceManager: Static utility for managing Sound/Music categories (Player, Enemy, UI) and cooldowns to prevent audio spam.
            Hud: Manages the UI overlay (Health hearts, XP bars, Compass).
         	AchievementHud: Handles the sliding popup notifications.
         	ScoreToast: Floating text numbers when points are gained.
         	DevConsole: Handles debug commands (God mode, spawning, etc.).



## **4. Key Relationships**



Rendering Flow:

    MazeRunnerGame -> GameScreen -> render() -> MapManager.render() (Background/Walls) -> GameObject.draw() (Entities sorted by Y-position) -> Hud.draw() (UI).

Input Flow:

    InputAdapter (in GameScreen) -> Player.keyDown() -> InputStack (handles smooth movement direction changes).
        Note: The DevConsole can intercept input when active.

Collision Flow:

    GameScreen update loop -> Checks Player vs Items -> Checks Player vs Obstacles -> Checks MageBolt vs Player.
    
    Wall collisions are handled inside Player/Enemy.update() via MapManager.isCollidingWithWall().

Audio Flow:

    Events (e.g., Player.takeDamage) -> VoiceManager.playPlayer("hurt") -> Checks GamePreferences for volume/mute settings -> Plays LibGDX Sound.









# **How to Run and Use the Game**

1. Open Intellij
2. Clone Repository -> https://go97jus@artemis.tum.de/git/FOPWS2526PROJECTFOP/fopws2526projectfop-tumato.git -> Clone
3. In the project, edit run configuration.

        choose abcd.desktop.main as the module whose classpath should be used in the run application
        
        (if on macbook, in edit configuration -> add vm options -> paste "-XstartOnFirstThread" -> Apply

4\. Run the code





# **The Game Itself**


    After surviving a brutal battlefield of monsters and lightning, Theseus knelt before the King of Crete to receive the scroll that would seal his fate.

    The King granted him the title of knight on one condition: he must descend into the dark, jagged maw of the Labyrinth, Knossos, and conquer the ancient evil dwelling within.

    With a heavy heart and a flickering torch, Theseus stood before the rain-slicked entrance, knowing that his path to true honor lay deep beneath the stone.



To play the game, press "Play" and choose a mode
Story is the basic mode based on pre-built map files

Endless mode is a randomly generated maze using Prim's algorithm that scales in difficulty

Press "esc" while in story or endless to pause the game and enter the pause menu.

In story mode, the user has 4 save files they can use. Click on new game to create a save file and start a new game. The game automatically saves every 30 seconds and the player can save and quit in the pause menu to keep progress. Press the reset button in the save selection menu to delete the current run and start a new game.

The leaderboard tracks the 10 best scores in the endless mode.

Mastery is used to permanently upgrade the character using skill points. Kill enemies to collect experience. 100xp = 1 skill point

    Upgrades include:
    Faster dashing cooldown and invincible dash (no dammage while dashing)
    Faster default speed, faster stamina regeneration and unlimited stamina
    Strength upgrades including One-Hit-KO
    Health regeneration upgrades


Achievements on the menu screen are tracked and stored in a json file. You can see your achievements in the menu and press "Reset All" to reset the achievements to the beginning.

The story button on the main menu plays the intro cutscene again


Control the volume of the game (music and SFX volume) using the sliders in the option menu. Go to keyboard configuration in the options menu to set custom keybindings to control the game

The player has 20 lives. In story mode, the player must collect the key and exit the map (exit is locked until the key is collected) until the final level to win. Losing deletes the current save file.
In endless mode, the player keeps playing until they lose all lives. Their score is then stored in the Leaderboards screen if it is scored higher than any of the stored entries.



# **Cheats**
Enter these in the dev console (press T while in game) to activate cheats



    Toggle ghost mode (walk through walls)
    /ghost

    Toggle immortality (can't take damage)
    /immortal

    Give a very long speed boost (9999 seconds, basically permanent)
    /speed

    Add one extra life
    /addhealth

    Instantly obtain the key without finding it
    /getkey

    Skip to the next level immediately
    /nextlevel

    Kill all enemies currently on the map
    /killall

    Clear all active cheats and buffs (reset to normal state)
    /clear

    Give 20 skill points
    /giveexp




# **Credits**
fantasy_ by Kevin's Mom - Wall tiles

All-In-One Dungeon Asset Pack by Jak_96 - Sprite Pack (paid for)

SFX - FMOD & incompetech

Images - Generated with Gemini Nano Banana Pro



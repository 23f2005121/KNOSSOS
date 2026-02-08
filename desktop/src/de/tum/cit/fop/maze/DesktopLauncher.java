package de.tum.cit.fop.maze;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import games.spooky.gdx.nativefilechooser.desktop.DesktopFileChooser;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("Maze Runner");
        config.setWindowedMode(1280, 720);

        // Anti-aliasing
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);

        new Lwjgl3Application(new MazeRunnerGame(new DesktopFileChooser()), config);
    }
}

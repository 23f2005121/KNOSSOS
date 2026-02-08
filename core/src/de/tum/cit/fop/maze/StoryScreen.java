/**
 * Is responsible for playing the cinematic shown in the beginning of the game,
 * the cinematic itself comprises 3 images with some lore
 */

package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class StoryScreen implements Screen {



    private final MazeRunnerGame game;
    private final Stage stage;
    private final Texture[] images;
    private final String[] texts;
    private int currentIndex = 0;

    private Label narrativeLabel;

    // Background image actor
    private com.badlogic.gdx.scenes.scene2d.ui.Image currentImageActor;

    public StoryScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        // Load images
        images = new Texture[3];
        try {
            images[0] = new Texture(Gdx.files.internal("intro_1.png"));
            images[1] = new Texture(Gdx.files.internal("intro_2.png"));
            images[2] = new Texture(Gdx.files.internal("intro_3.png"));
        } catch (Exception e) {
            Gdx.app.error("StoryScreen", "Failed to load intro images", e);
            // Fallback or handle error - skipping story if images fail
            startGame();
            // Initialize empty arrays to prevent crashes in dispose/setupUI if we proceed
            // But since we called startGame, we essentially exit this screen flow.
            // However, the constructor must finish.
            this.texts = new String[0];
            return;
        }

        // Narrative text
        texts = new String[] {
                "After surviving a brutal battlefield of monsters and lightning, Theseus knelt before the King of Crete to receive the scroll that would seal his fate.",
                "The King granted him the title of knight on one condition: he must descend into the dark, jagged maw of the Labyrinth, Knossos, and conquer the ancient evil dwelling within.",
                "With a heavy heart and a flickering torch, Theseus stood before the rain-slicked entrance, knowing that his path to true honor lay deep beneath the stone."
        };

        setupUI();
    }

    private void setupUI() {
        if (images[0] == null)
            return; // Safety check

        stage.clear();

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Stack to overlay text on image
        com.badlogic.gdx.scenes.scene2d.ui.Stack stack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
        mainTable.add(stack).grow();

        // 1. The Image Layer
        currentImageActor = new com.badlogic.gdx.scenes.scene2d.ui.Image(images[0]);
        currentImageActor.setScaling(com.badlogic.gdx.utils.Scaling.fill);
        stack.add(currentImageActor);

        // 2. The UI Layer
        Table uiTable = new Table();
        uiTable.setFillParent(true);
        stack.add(uiTable);

        // Top Right - Skip Button
        TextButton skipButton = new TextButton("Skip", game.getSkin());
        skipButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                startGame();
            }
        });

        Table topTable = new Table();
        topTable.top().right();
        topTable.add(skipButton).pad(20);
        uiTable.add(topTable).growX().top().row();

        // Spacer
        uiTable.add().grow().row();

        // Bottom - Narrative Text and Next Button
        Table bottomTable = new Table();
        // Check if "white" exists, otherwise use a default or skip background
        try {
            bottomTable.setBackground(game.getSkin().newDrawable("white", 0, 0, 0, 0.7f));
        } catch (Exception e) {
            // Fallback: no background or try another logical name if 'white' fails
            // Just ignore background if fails
        }

        narrativeLabel = new Label(texts[0], game.getSkin());
        narrativeLabel.setWrap(true);
        narrativeLabel.setAlignment(Align.center);

        bottomTable.add(narrativeLabel).growX().pad(20).center();

        TextButton nextButton = new TextButton("Next", game.getSkin());
        nextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VoiceManager.playUI("ui_click");
                nextSlide();
            }
        });
        bottomTable.add(nextButton).pad(20).right();

        uiTable.add(bottomTable).growX().bottom();
    }

    private void nextSlide() {
        currentIndex++;
        if (currentIndex >= images.length) {
            startGame();
        } else {
            updateSlide();
        }
    }

    private void updateSlide() {
        if (currentImageActor != null) {
            currentImageActor.setDrawable(new TextureRegionDrawable(images[currentIndex]));
        }
        if (narrativeLabel != null) {
            narrativeLabel.setText(texts[currentIndex]);
        }
    }

    private void startGame() {
        game.goToMenu();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        // Only dispose if we successfully loaded them
        if (images != null) {
            for (Texture t : images) {
                if (t != null)
                    t.dispose();
            }
        }
    }
}

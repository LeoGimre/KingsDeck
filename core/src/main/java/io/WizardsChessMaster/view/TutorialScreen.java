package io.WizardsChessMaster.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.presenter.TutorialPresenter;
import io.WizardsChessMaster.model.tutorials.TutorialConfig;
import io.WizardsChessMaster.view.interfaces.ITutorialView;

/**
 * View component for the Tutorial Screen.
 * Displays a list of topics and the content for the selected topic.
 * Implements ITutorialView for interaction with TutorialController.
 */
public class TutorialScreen extends ScreenAdapter implements ITutorialView {

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final TutorialPresenter controller;

    private List<String> topicListWidget;
    private ScrollPane topicScrollPane;
    private Label contentTitleLabel;
    private Label contentTextLabel;
    private ScrollPane contentScrollPane;
    private Table contentTable;

    public TutorialScreen(final Main game, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        if (sharedSkin == null) {
            Gdx.app.error("TutorialScreen", "Error: Shared skin is null!");
            throw new RuntimeException("Shared skin cannot be null for TutorialScreen");
        }
        this.skin = sharedSkin;

        this.controller = new TutorialPresenter(game);
        setupUi();
    }

    private void setupUi() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(20);
        Drawable tableBg = skin.getDrawable("window");
        if (tableBg != null) {
            mainTable.setBackground(tableBg);
        } else {
            mainTable.setBackground(skin.newDrawable("white", Color.DARK_GRAY));
        }
        stage.addActor(mainTable);

        // --- Left Panel (Topic List) ---
        Table leftTable = new Table();
        leftTable.top();
        topicListWidget = new List<>(skin);
        topicScrollPane = new ScrollPane(topicListWidget, skin);
        topicScrollPane.setFadeScrollBars(false);
        topicScrollPane.setScrollingDisabled(true, false);

        leftTable.add(new Label("Topics", skin, "subtitle")).padBottom(15).row();
        leftTable.add(topicScrollPane).growY().width(Value.percentWidth(0.95f, leftTable)).padTop(5);

        // --- Right Panel (Content Display) ---
        contentTable = new Table();
        contentTable.pad(15);
        contentTable.top().left();

        contentTitleLabel = new Label("", skin, "subtitle");
        contentTitleLabel.setAlignment(Align.center);
        contentTitleLabel.setWrap(true);
        contentTextLabel = new Label("", skin);
        contentTextLabel.setWrap(true);
        contentTextLabel.setAlignment(Align.topLeft);

        contentTable.add(contentTitleLabel).growX().center().padBottom(20).row();
        contentTable.add(contentTextLabel).grow().left().top();

        contentScrollPane = new ScrollPane(contentTable, skin);
        contentScrollPane.setFadeScrollBars(false);
        contentScrollPane.setScrollingDisabled(true, false);
        Drawable contentBg = skin.getDrawable("list");
        if (contentBg != null) {
            contentScrollPane.setStyle(new ScrollPane.ScrollPaneStyle(skin.get(ScrollPane.ScrollPaneStyle.class)));
            contentScrollPane.getStyle().background = contentBg;
        }

        // --- Back Button ---
        TextButton backButton = new TextButton("Back to Menu", skin);

        // --- Layout using mainTable columns ---
        mainTable.add(new Label("Tutorial", skin, "title")).colspan(2).spaceBottom(25).center().row();
        mainTable.add(leftTable).width(Value.percentWidth(0.3f, mainTable)).growY().pad(10);
        mainTable.add(contentScrollPane).grow().pad(10);
        mainTable.row();
        mainTable.add(backButton).colspan(2).padTop(25).padBottom(10).center();

        // --- Controller Interactions ---
        topicListWidget.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int selectedIndex = topicListWidget.getSelectedIndex();
                if (selectedIndex != -1 && controller != null) {
                    controller.selectTopic(selectedIndex);
                }
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) controller.handleGoBack();
            }
        });
    }

    // --- ITutorialView Implementation ---

    @Override
    public void updateContent(TutorialConfig topicConfig) {
        if (topicConfig != null) {
            contentTitleLabel.setText(topicConfig.getTitle());
            contentTextLabel.setText(topicConfig.getContent());
            contentTable.invalidateHierarchy();
            contentScrollPane.layout();
            Gdx.app.postRunnable(() -> contentScrollPane.setScrollY(0));
            Gdx.app.log("TutorialScreen", "Updated content for: " + topicConfig.getTitle());
        } else {
            contentTitleLabel.setText("Error");
            contentTextLabel.setText("Could not load selected topic.");
            Gdx.app.error("TutorialScreen", "Attempted to update content with a null topic config.");
            contentTable.invalidateHierarchy();
        }
    }

    @Override
    public void setTopicTitles(String[] topicTitles) {
        if (topicListWidget != null && topicTitles != null) {
            topicListWidget.setItems(new Array<>(topicTitles));
        } else if (topicListWidget != null) {
            topicListWidget.setItems(new Array<String>());
        }
    }

    // --- Screen Lifecycle Methods ---

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        if (controller != null) {
            controller.setView(this);
        }
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        stage.dispose();
        Gdx.app.log("TutorialScreen", "Disposed.");
    }
}
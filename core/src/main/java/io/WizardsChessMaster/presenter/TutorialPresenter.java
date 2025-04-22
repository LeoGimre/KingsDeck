package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.model.tutorials.TutorialConfig;
import io.WizardsChessMaster.model.tutorials.TutorialModel;
import io.WizardsChessMaster.view.interfaces.ITutorialView;


/**
 * Controller for handling logic related to the TutorialScreen.
 * Now interacts with the view via the ITutorialView interface.
 */
public class TutorialPresenter {

    private final Main game;
    private final TutorialModel model;
    private ITutorialView view;

    public TutorialPresenter(Main game) {
        this.game = game;
        this.model = new TutorialModel();
        this.model.loadTopics();
    }

    public void setView(ITutorialView view) {
        this.view = view;
        initializeView();
    }

    /**
     * Initializes the view with initial data.
     */
    public void initializeView() {
        if (view != null && model != null) {
            view.setTopicTitles(model.getTopicTitles());
            if (model.getCurrentTopic() != null) {
                view.updateContent(model.getCurrentTopic());
            } else {
                Gdx.app.error("TutorialPresenter", "Model's current topic is null during view initialization.");
            }
        } else {
            Gdx.app.error("TutorialPresenter", "View or Model is null during initializeView.");
        }
    }


    public TutorialModel getModel() {
        return model;
    }

    /**
     * Handles the selection of a tutorial topic from the list.
     * @param index The index of the selected topic.
     */
    public void selectTopic(int index) {
        Gdx.app.log("TutorialController", "Topic selected at index: " + index);
        model.setCurrentTopic(index);
        TutorialConfig selectedConfig = model.getCurrentTopic();
        if (view != null && selectedConfig != null) {
            view.updateContent(selectedConfig);
        } else {
            Gdx.app.error("TutorialController", "View is null or topic config is null, cannot update content.");
        }
    }

    /**
     * Handles the action to go back, usually to the main menu.
     */
    public void handleGoBack() {
        Gdx.app.log("TutorialController", "Go Back requested.");
        game.showMainMenuScreen();
    }
}
package io.WizardsChessMaster.view.interfaces;

import io.WizardsChessMaster.model.tutorials.TutorialConfig;

/**
 * Interface defining the methods a TutorialController can use
 * to interact with its corresponding View (TutorialScreen).
 */
public interface ITutorialView {

    /**
     * Updates the content display area (title and text) with the
     * details of the provided tutorial topic configuration.
     * @param topicConfig The TutorialConfig data to display.
     */
    void updateContent(TutorialConfig topicConfig);

    /**
     * Populates the list widget with the available topic titles.
     * @param topicTitles An array of strings representing the titles.
     */
    void setTopicTitles(String[] topicTitles);

}
package io.WizardsChessMaster.model.tutorials;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * Model holding the data for the tutorial screen.
 * Loads topics from TutorialFactory.
 */
public class TutorialModel {
    private static final String TAG = "TutorialModel";

    private final List<TutorialConfig> topics;
    private TutorialConfig currentTopic;

    public TutorialModel() {
        topics = new ArrayList<>();
        currentTopic = null;
    }

    /**
     * Loads tutorial topics from the TutorialFactory.
     * Should be called after TutorialFactory is initialized.
     */
    public void loadTopics() {
        topics.clear();
        topics.addAll(TutorialFactory.getAllConfigsSortedByTitle());

        // Sort topics to ensure "Welcome" is always first
        topics.sort((a, b) -> {
            if ("Welcome".equalsIgnoreCase(a.getTopicId())) return -1;
            if ("Welcome".equalsIgnoreCase(b.getTopicId())) return 1;
            return a.getTopicId().compareToIgnoreCase(b.getTopicId());
        });

        if (!topics.isEmpty()) {
            currentTopic = topics.get(0);
            Gdx.app.log(TAG, "Loaded " + topics.size() + " tutorial topics. Default: " + currentTopic.getTitle());
        } else {
            currentTopic = new TutorialConfig();
            currentTopic.topicId = "error";
            currentTopic.title = "Error";
            currentTopic.content = "No tutorial topics found. Please check the 'assets/tutorials' directory.";
            Gdx.app.error(TAG, "No tutorial topics found!");
        }
    }

    public List<TutorialConfig> getTopics() {
        return topics;
    }

    public String[] getTopicTitles() {
        String[] titles = new String[topics.size()];
        for (int i = 0; i < topics.size(); i++) {
            titles[i] = topics.get(i).getTitle();
        }
        return titles;
    }

    public TutorialConfig getCurrentTopic() {
        return currentTopic;
    }

    public void setCurrentTopic(int index) {
        if (index >= 0 && index < topics.size()) {
            this.currentTopic = topics.get(index);
        } else {
            Gdx.app.error(TAG, "Invalid index for setCurrentTopic: " + index);
        }
    }

    public TutorialConfig getTopic(int index) {
        if (index >= 0 && index < topics.size()) {
            return topics.get(index);
        }
        return null;
    }
}
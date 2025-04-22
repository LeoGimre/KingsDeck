package io.WizardsChessMaster.model.tutorials;

import java.util.Objects;

/**
 * Represents the configuration data for a single tutorial topic,
 * loaded from a JSON file.
 */
public class TutorialConfig {

    // Fields matching the desired JSON structure
    public String topicId;
    public String title;
    public String content;
    public String imagePath;

    // Default constructor for JSON parsing
    public TutorialConfig() {
    }

    // --- Getters ---

    public String getTopicId() {
        return topicId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImagePath() {
        return imagePath;
    }

    // --- Standard Java methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TutorialConfig that = (TutorialConfig) o;
        return Objects.equals(topicId, that.topicId) &&
                Objects.equals(title, that.title) &&
                Objects.equals(content, that.content) &&
                Objects.equals(imagePath, that.imagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, title, content, imagePath);
    }

    @Override
    public String toString() {
        return "TutorialConfig{" +
                "topicId='" + topicId + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}
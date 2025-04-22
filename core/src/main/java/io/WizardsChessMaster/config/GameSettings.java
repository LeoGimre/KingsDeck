// Create file: core/src/main/java/io/WizardsChessMaster/config/GameSettings.java
package io.WizardsChessMaster.config;

import java.util.List;
import java.util.ArrayList; // Import ArrayList

/**
 * Plain Old Java Object (POJO) to hold game settings loaded from JSON.
 */
public class GameSettings {

    // Inner class for Matchmaking settings
    public static class MatchmakingSettings {
        public List<Integer> pointLimits = new ArrayList<>(); // Initialize to prevent null
        public List<String> timeLimits = new ArrayList<>();   // Initialize to prevent null
        public String defaultTimeLimit = "5 min";
        public Integer defaultPointLimit = 40;
    }

    // Inner class for Deck Building settings
    public static class DeckBuildingSettings {
        public List<Integer> pointLimits = new ArrayList<>(); // Initialize to prevent null
        public Integer defaultPointLimit = 40;
    }

    // Top-level fields matching JSON structure
    public MatchmakingSettings matchmaking = new MatchmakingSettings(); // Initialize to prevent null
    public DeckBuildingSettings deckBuilding = new DeckBuildingSettings(); // Initialize to prevent null

    // No-arg constructor required for JSON parsing
    public GameSettings() {}
}
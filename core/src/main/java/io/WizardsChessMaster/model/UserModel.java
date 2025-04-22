package io.WizardsChessMaster.model;

/**
 * Model class to hold user profile data.
 */
public class UserModel {
    private String userId;
    private String displayName;
    private int eloRating;
    private int gamesPlayed;
    private int gamesWon;

    public UserModel() {
    }

    public UserModel(String userId, String displayName, int eloRating, int gamesPlayed, int gamesWon) {
        this.userId = userId;
        this.displayName = displayName;
        this.eloRating = eloRating;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getEloRating() {
        return eloRating;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UserModel{" +
            "userId='" + userId + '\'' +
            ", displayName='" + displayName + '\'' +
            ", eloRating=" + eloRating +
            ", gamesPlayed=" + gamesPlayed +
            ", gamesWon=" + gamesWon +
            '}';
    }
}


package io.WizardsChessMaster.model;

public enum GameStatus {
    PENDING_JOIN("pending_join"),
    PENDING_CODE_JOIN("pending_code_join"),
    ACTIVE("active"),
    FINISHED("finished"),
    ERROR("error");

    private final String firestoreValue;

    GameStatus(String firestoreValue) {
        this.firestoreValue = firestoreValue;
    }

    /**
     * Gets the string value used for storing in Firestore.
     * @return The Firestore string representation.
     */
    public String getFirestoreValue() {
        return firestoreValue;
    }

    /**
     * Converts a Firestore string value back to the Enum.
     * Returns null if the value doesn't match any status.
     * @param value The string value from Firestore.
     * @return The corresponding GameStatus enum, or null if not found.
     */
    public static GameStatus fromFirestoreValue(String value) {
        if (value == null) return null;
        for (GameStatus status : values()) {
            if (status.firestoreValue.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Checks if the game is considered "over" (finished or error).
     * @return true if the game status is FINISHED or ERROR.
     */
    public boolean isGameOver() {
        return this == FINISHED || this == ERROR;
    }

    /**
     * Checks if the game is actively being played.
     * @return true if the game status is ACTIVE.
     */
    public boolean isActive() { return this == ACTIVE; }

    /**
     * Checks if the game is waiting for any player to join (ranked or hosted).
     * @return true if status is PENDING_JOIN or PENDING_CODE_JOIN.
     */
    public boolean isPendingJoin() { return this == PENDING_JOIN || this == PENDING_CODE_JOIN; }
}
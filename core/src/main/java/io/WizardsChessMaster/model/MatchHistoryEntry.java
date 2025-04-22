package io.WizardsChessMaster.model;

/**
 * Represents a single entry in the user's match history.
 */
public class MatchHistoryEntry {
    private final String opponentDisplayName;
    private final String result;
    private final String eloChange;
    private final long timestamp;

    public MatchHistoryEntry(String opponentDisplayName, String result, String eloChange, long timestamp) {
        this.opponentDisplayName = opponentDisplayName;
        this.result = result;
        this.eloChange = eloChange;
        this.timestamp = timestamp;
    }

    public String getOpponentDisplayName() {
        return opponentDisplayName != null ? opponentDisplayName : "Unknown";
    }

    public String getResult() {
        return result != null ? result : "-";
    }

    public String getEloChange() {
        return eloChange != null ? eloChange : "-";
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MatchHistoryEntry{" +
                "opponent='" + opponentDisplayName + '\'' +
                ", result='" + result + '\'' +
                ", eloChange='" + eloChange + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
package io.WizardsChessMaster.model;

/**
 * Represents the team or color of a player or piece.
 */
public enum Team {
    WHITE, BLACK;

    /**
     * Gets the opposing team.
     * @return The opposite Team.
     */
    public Team opposite() {
        return (this == WHITE) ? BLACK : WHITE;
    }
}

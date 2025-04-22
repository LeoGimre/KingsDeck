package io.WizardsChessMaster.model;

import java.util.Objects;

/**
 * Represents a position on the chessboard using (x, y) coordinates.
 * Assumes (0,0) is a corner (e.g., bottom-left).
 */
public class BoardPosition {
    private final int x;
    private final int y;

    public BoardPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Creates a new position by adding dx and dy to the current position.
     * @param dx Change in x.
     * @param dy Change in y.
     * @return A new BoardPosition instance.
     */
    public BoardPosition add(int dx, int dy) {
        return new BoardPosition(this.x + dx, this.y + dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardPosition that = (BoardPosition) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

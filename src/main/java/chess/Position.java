package chess;

public class Position {
    public final int row; // 0-7
    public final int col; // 0-7

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public boolean isOnBoard() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() { return row * 8 + col; }
}
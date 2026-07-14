package chess;

public class Evaluator {
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;

    // Piece-square tables: bonus/penalty (in centipawns) for a piece standing
    // on a given square. Tables are written from White's perspective, with
    // row 0 = White's back rank (a1-h1) and row 7 = Black's back rank (a8-h8).
    // For Black pieces, we mirror the table vertically (see mirrorRow below).

    private static final int[][] PAWN_TABLE = {
            {  0,   0,   0,   0,   0,   0,   0,   0},
            {  5,  10,  10, -20, -20,  10,  10,   5},
            {  5,  -5, -10,   0,   0, -10,  -5,   5},
            {  0,   0,   0,  20,  20,   0,   0,   0},
            {  5,   5,  10,  25,  25,  10,   5,   5},
            { 10,  10,  20,  30,  30,  20,  10,  10},
            { 50,  50,  50,  50,  50,  50,  50,  50},
            {  0,   0,   0,   0,   0,   0,   0,   0}
    };

    private static final int[][] KNIGHT_TABLE = {
            {-50, -40, -30, -30, -30, -30, -40, -50},
            {-40, -20,   0,   5,   5,   0, -20, -40},
            {-30,   5,  10,  15,  15,  10,   5, -30},
            {-30,   0,  15,  20,  20,  15,   0, -30},
            {-30,   5,  15,  20,  20,  15,   5, -30},
            {-30,   0,  10,  15,  15,  10,   0, -30},
            {-40, -20,   0,   0,   0,   0, -20, -40},
            {-50, -40, -30, -30, -30, -30, -40, -50}
    };

    private static final int[][] BISHOP_TABLE = {
            {-20, -10, -10, -10, -10, -10, -10, -20},
            {-10,   5,   0,   0,   0,   0,   5, -10},
            {-10,  10,  10,  10,  10,  10,  10, -10},
            {-10,   0,  10,  10,  10,  10,   0, -10},
            {-10,   5,   5,  10,  10,   5,   5, -10},
            {-10,   0,   5,  10,  10,   5,   0, -10},
            {-10,   0,   0,   0,   0,   0,   0, -10},
            {-20, -10, -10, -10, -10, -10, -10, -20}
    };

    private static final int[][] ROOK_TABLE = {
            {  0,   0,   0,   5,   5,   0,   0,   0},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            {  5,  10,  10,  10,  10,  10,  10,   5},
            {  0,   0,   0,   0,   0,   0,   0,   0}
    };

    private static final int[][] QUEEN_TABLE = {
            {-20, -10, -10,  -5,  -5, -10, -10, -20},
            {-10,   0,   5,   0,   0,   0,   0, -10},
            {-10,   5,   5,   5,   5,   5,   0, -10},
            {  0,   0,   5,   5,   5,   5,   0,  -5},
            { -5,   0,   5,   5,   5,   5,   0,  -5},
            {-10,   0,   5,   5,   5,   5,   0, -10},
            {-10,   0,   0,   0,   0,   0,   0, -10},
            {-20, -10, -10,  -5,  -5, -10, -10, -20}
    };

    private static final int[][] KING_TABLE = {
            { 20,  30,  10,   0,   0,  10,  30,  20},
            { 20,  20,   0,   0,   0,   0,  20,  20},
            {-10, -20, -20, -20, -20, -20, -20, -10},
            {-20, -30, -30, -40, -40, -30, -30, -20},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30}
    };
    // NOTE: this King table favors safety/castling and is meant for the
    // middlegame. A more advanced engine swaps to an "endgame king table"
    // once few pieces remain — a good stretch goal for later.

    public int evaluate(Board board) {
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = board.getPieceAt(new Position(row, col));
                if (p == null) continue;

                int material = pieceValue(p.getType());
                int positional = positionValue(p.getType(), row, col, p.getColor());
                int total = material + positional;

                score += (p.getColor() == Color.WHITE) ? total : -total;
            }
        }
        return score;
    }

    private int pieceValue(PieceType type) {
        return switch (type) {
            case PAWN -> PAWN_VALUE;
            case KNIGHT -> KNIGHT_VALUE;
            case BISHOP -> BISHOP_VALUE;
            case ROOK -> ROOK_VALUE;
            case QUEEN -> QUEEN_VALUE;
            case KING -> 0;
        };
    }

    private int positionValue(PieceType type, int row, int col, Color color) {
        // Black's tables are mirrored vertically, since the tables above are
        // written from White's perspective (row 0 = White's back rank).
        int lookupRow = (color == Color.WHITE) ? row : (7 - row);

        int[][] table = switch (type) {
            case PAWN -> PAWN_TABLE;
            case KNIGHT -> KNIGHT_TABLE;
            case BISHOP -> BISHOP_TABLE;
            case ROOK -> ROOK_TABLE;
            case QUEEN -> QUEEN_TABLE;
            case KING -> KING_TABLE;
        };
        return table[lookupRow][col];
    }
}
package chess;

public class Board {
    private final Piece[][] grid = new Piece[8][8]; // grid[row][col]
    private Color turn = Color.WHITE;
    // TODO: castling rights, en passant target square — added later

    public Board() {
        setupStartingPosition();
    }

    private void setupStartingPosition() {
        for (int col = 0; col < 8; col++) {
            grid[1][col] = new Piece(PieceType.PAWN, Color.WHITE);
            grid[6][col] = new Piece(PieceType.PAWN, Color.BLACK);
        }
        PieceType[] backRank = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int col = 0; col < 8; col++) {
            grid[0][col] = new Piece(backRank[col], Color.WHITE);
            grid[7][col] = new Piece(backRank[col], Color.BLACK);
        }
    }

    public Piece getPieceAt(Position p) { return grid[p.row][p.col]; }
    public void setPieceAt(Position p, Piece piece) { grid[p.row][p.col] = piece; }
    public Color getTurn() { return turn; }
    public void switchTurn() { turn = turn.opposite(); }

    public void applyMove(Move move) {
        grid[move.to.row][move.to.col] = move.movedPiece;
        grid[move.from.row][move.from.col] = null;
        // NOTE: castling, en passant, promotion not yet handled here — added later
    }

    public void undoMove(Move move) {
        grid[move.from.row][move.from.col] = move.movedPiece;
        grid[move.to.row][move.to.col] = move.capturedPiece; // null if there was no capture
    }

    public Position findKing(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = grid[row][col];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return new Position(row, col);
                }
            }
        }
        return null; // should never happen in a valid game
    }

    // Is the given color's king currently in check?
    public boolean isInCheck(Color color, MoveGenerator generator) {
        Position kingPos = findKing(color);
        return generator.isSquareAttacked(this, kingPos, color.opposite());
    }

    // Checkmate: in check AND no legal moves available
    public boolean isCheckmate(Color color, MoveGenerator generator) {
        if (!isInCheck(color, generator)) return false;
        return generator.generateLegalMoves(this, color).isEmpty();
    }

    // Stalemate: NOT in check, but no legal moves available (draw)
    public boolean isStalemate(Color color, MoveGenerator generator) {
        if (isInCheck(color, generator)) return false;
        return generator.generateLegalMoves(this, color).isEmpty();
    }

    public void printBoard() {
        String[] symbols = {"P", "N", "B", "R", "Q", "K"};
        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                Piece p = grid[row][col];
                if (p == null) {
                    System.out.print(". ");
                } else {
                    String s = symbols[p.getType().ordinal()];
                    System.out.print((p.getColor() == Color.WHITE ? s : s.toLowerCase()) + " ");
                }
            }
            System.out.println();
        }
    }
}
package chess;

public class Board {
    private final Piece[][] grid = new Piece[8][8];
    private Color turn = Color.WHITE;

    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteRookAMoved = false;
    private boolean whiteRookHMoved = false;
    private boolean blackRookAMoved = false;
    private boolean blackRookHMoved = false;

    private Position enPassantTarget = null;

    public Board() {
        setupStartingPosition();
    }

    private Board(boolean skipSetup) {
    }

    public Board copy() {
        Board clone = new Board(true);
        for (int row = 0; row < 8; row++) {
            System.arraycopy(this.grid[row], 0, clone.grid[row], 0, 8);
        }
        clone.turn = this.turn;
        clone.whiteKingMoved = this.whiteKingMoved;
        clone.blackKingMoved = this.blackKingMoved;
        clone.whiteRookAMoved = this.whiteRookAMoved;
        clone.whiteRookHMoved = this.whiteRookHMoved;
        clone.blackRookAMoved = this.blackRookAMoved;
        clone.blackRookHMoved = this.blackRookHMoved;
        clone.enPassantTarget = this.enPassantTarget;
        return clone;
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
    public Position getEnPassantTarget() { return enPassantTarget; }

    public boolean canCastleKingside(Color color) {
        if (color == Color.WHITE) return !whiteKingMoved && !whiteRookHMoved;
        return !blackKingMoved && !blackRookHMoved;
    }

    public boolean canCastleQueenside(Color color) {
        if (color == Color.WHITE) return !whiteKingMoved && !whiteRookAMoved;
        return !blackKingMoved && !blackRookAMoved;
    }

    public void applyMove(Move move) {
        updateCastlingRights(move);

        if (move.isEnPassant) {
            int capturedPawnRow = move.from.row;
            grid[capturedPawnRow][move.to.col] = null;
        }

        if (move.isCastling) {
            int row = move.from.row;
            boolean kingside = move.to.col == 6;
            int rookFromCol = kingside ? 7 : 0;
            int rookToCol = kingside ? 5 : 3;
            Piece rook = grid[row][rookFromCol];
            grid[row][rookToCol] = rook;
            grid[row][rookFromCol] = null;
        }

        Piece pieceToPlace = move.movedPiece;
        if (move.promotionType != null) {
            pieceToPlace = new Piece(move.promotionType, move.movedPiece.getColor());
        }

        grid[move.to.row][move.to.col] = pieceToPlace;
        grid[move.from.row][move.from.col] = null;

        enPassantTarget = null;
        if (move.movedPiece.getType() == PieceType.PAWN
                && Math.abs(move.to.row - move.from.row) == 2) {
            int midRow = (move.to.row + move.from.row) / 2;
            enPassantTarget = new Position(midRow, move.from.col);
        }
    }

    private void updateCastlingRights(Move move) {
        PieceType type = move.movedPiece.getType();
        Color color = move.movedPiece.getColor();

        if (type == PieceType.KING) {
            if (color == Color.WHITE) whiteKingMoved = true; else blackKingMoved = true;
        }
        if (type == PieceType.ROOK) {
            if (color == Color.WHITE) {
                if (move.from.row == 0 && move.from.col == 0) whiteRookAMoved = true;
                if (move.from.row == 0 && move.from.col == 7) whiteRookHMoved = true;
            } else {
                if (move.from.row == 7 && move.from.col == 0) blackRookAMoved = true;
                if (move.from.row == 7 && move.from.col == 7) blackRookHMoved = true;
            }
        }
        if (move.to.row == 0 && move.to.col == 0) whiteRookAMoved = true;
        if (move.to.row == 0 && move.to.col == 7) whiteRookHMoved = true;
        if (move.to.row == 7 && move.to.col == 0) blackRookAMoved = true;
        if (move.to.row == 7 && move.to.col == 7) blackRookHMoved = true;
    }

    public void undoMove(Move move) {
        if (move.isCastling) {
            int row = move.from.row;
            boolean kingside = move.to.col == 6;
            int rookFromCol = kingside ? 7 : 0;
            int rookToCol = kingside ? 5 : 3;
            Piece rook = grid[row][rookToCol];
            grid[row][rookFromCol] = rook;
            grid[row][rookToCol] = null;
        }

        grid[move.from.row][move.from.col] = move.movedPiece;
        grid[move.to.row][move.to.col] = move.capturedPiece;

        if (move.isEnPassant) {
            grid[move.from.row][move.to.col] = move.capturedPiece;
            grid[move.to.row][move.to.col] = null;
        }
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
        return null;
    }

    public boolean isInCheck(Color color, MoveGenerator generator) {
        Position kingPos = findKing(color);
        return generator.isSquareAttacked(this, kingPos, color.opposite());
    }

    public boolean isCheckmate(Color color, MoveGenerator generator) {
        if (!isInCheck(color, generator)) return false;
        return generator.generateLegalMoves(this, color).isEmpty();
    }

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
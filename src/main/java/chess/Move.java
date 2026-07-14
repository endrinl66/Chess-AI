package chess;

public class Move {
    public final Position from;
    public final Position to;
    public final Piece movedPiece;
    public final Piece capturedPiece; // null if no capture
    public final boolean isCastling;
    public final boolean isEnPassant;
    public final PieceType promotionType; // null unless pawn promotes

    public Move(Position from, Position to, Piece movedPiece, Piece capturedPiece,
                boolean isCastling, boolean isEnPassant, PieceType promotionType) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;
        this.promotionType = promotionType;
    }
}
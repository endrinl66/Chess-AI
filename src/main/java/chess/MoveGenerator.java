package chess;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {

    private static final int[][] KNIGHT_OFFSETS = {
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    private static final int[][] BISHOP_DIRECTIONS = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final int[][] ROOK_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final int[][] QUEEN_DIRECTIONS = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private static final int[][] KING_OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public List<Move> generateKnightMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        Piece knight = board.getPieceAt(from);

        for (int[] offset : KNIGHT_OFFSETS) {
            Position to = new Position(from.row + offset[0], from.col + offset[1]);
            if (!to.isOnBoard()) continue;

            Piece target = board.getPieceAt(to);
            if (target == null || target.getColor() != knight.getColor()) {
                moves.add(new Move(from, to, knight, target, false, false, null));
            }
        }
        return moves;
    }

    public List<Move> generateSlidingMoves(Board board, Position from, int[][] directions) {
        List<Move> moves = new ArrayList<>();
        Piece piece = board.getPieceAt(from);

        for (int[] dir : directions) {
            int row = from.row;
            int col = from.col;

            while (true) {
                row += dir[0];
                col += dir[1];
                Position to = new Position(row, col);
                if (!to.isOnBoard()) break;

                Piece target = board.getPieceAt(to);
                if (target == null) {
                    moves.add(new Move(from, to, piece, null, false, false, null));
                } else {
                    if (target.getColor() != piece.getColor()) {
                        moves.add(new Move(from, to, piece, target, false, false, null));
                    }
                    break;
                }
            }
        }
        return moves;
    }

    public List<Move> generateBishopMoves(Board board, Position from) {
        return generateSlidingMoves(board, from, BISHOP_DIRECTIONS);
    }

    public List<Move> generateRookMoves(Board board, Position from) {
        return generateSlidingMoves(board, from, ROOK_DIRECTIONS);
    }

    public List<Move> generateQueenMoves(Board board, Position from) {
        return generateSlidingMoves(board, from, QUEEN_DIRECTIONS);
    }

    public List<Move> generatePawnMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        Piece pawn = board.getPieceAt(from);
        Color color = pawn.getColor();

        int direction = (color == Color.WHITE) ? 1 : -1;
        int startRow = (color == Color.WHITE) ? 1 : 6;

        Position oneForward = new Position(from.row + direction, from.col);
        if (oneForward.isOnBoard() && board.getPieceAt(oneForward) == null) {
            moves.add(new Move(from, oneForward, pawn, null, false, false, null));

            if (from.row == startRow) {
                Position twoForward = new Position(from.row + 2 * direction, from.col);
                if (board.getPieceAt(twoForward) == null) {
                    moves.add(new Move(from, twoForward, pawn, null, false, false, null));
                }
            }
        }

        int[] captureCols = {from.col - 1, from.col + 1};
        for (int col : captureCols) {
            Position capturePos = new Position(from.row + direction, col);
            if (!capturePos.isOnBoard()) continue;

            Piece target = board.getPieceAt(capturePos);
            if (target != null && target.getColor() != color) {
                moves.add(new Move(from, capturePos, pawn, target, false, false, null));
            }
        }

        // NOTE: en passant and promotion not yet handled — added later
        return moves;
    }

    public List<Move> generateKingMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        Piece king = board.getPieceAt(from);

        for (int[] offset : KING_OFFSETS) {
            Position to = new Position(from.row + offset[0], from.col + offset[1]);
            if (!to.isOnBoard()) continue;

            Piece target = board.getPieceAt(to);
            if (target == null || target.getColor() != king.getColor()) {
                moves.add(new Move(from, to, king, target, false, false, null));
            }
        }
        return moves;
        // NOTE: castling not yet handled — added later
    }

    public List<Move> generateMovesForPiece(Board board, Position from) {
        Piece piece = board.getPieceAt(from);
        if (piece == null) return new ArrayList<>();

        return switch (piece.getType()) {
            case PAWN -> generatePawnMoves(board, from);
            case KNIGHT -> generateKnightMoves(board, from);
            case BISHOP -> generateBishopMoves(board, from);
            case ROOK -> generateRookMoves(board, from);
            case QUEEN -> generateQueenMoves(board, from);
            case KING -> generateKingMoves(board, from);
        };
    }

    // Pseudo-legal: follows movement rules but may leave own king in check
    public List<Move> generateAllMoves(Board board, Color color) {
        List<Move> allMoves = new ArrayList<>();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                Piece piece = board.getPieceAt(pos);

                if (piece != null && piece.getColor() == color) {
                    allMoves.addAll(generateMovesForPiece(board, pos));
                }
            }
        }
        return allMoves;
    }

    // Is this square attacked by any piece of attackerColor?
    // NOTE: for pawns this only detects attacks on OCCUPIED squares (since pawn
    // captures require a target piece). Fine for king-safety checks since the
    // king always occupies its square; would need adjusting for other uses
    // like castling-through-check later.
    public boolean isSquareAttacked(Board board, Position square, Color attackerColor) {
        List<Move> attackerMoves = generateAllMoves(board, attackerColor);
        for (Move move : attackerMoves) {
            if (move.to.equals(square)) {
                return true;
            }
        }
        return false;
    }

    // Truly legal moves: pseudo-legal moves that don't leave your own king in check
    public List<Move> generateLegalMoves(Board board, Color color) {
        List<Move> pseudoLegalMoves = generateAllMoves(board, color);
        List<Move> legalMoves = new ArrayList<>();

        for (Move move : pseudoLegalMoves) {
            board.applyMove(move);
            Position kingPos = board.findKing(color);
            boolean stillInCheck = isSquareAttacked(board, kingPos, color.opposite());
            board.undoMove(move);

            if (!stillInCheck) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }
}
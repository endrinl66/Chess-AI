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

    private static final PieceType[] PROMOTION_CHOICES = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
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
        int promotionRow = (color == Color.WHITE) ? 7 : 0;

        // 1. Move forward one square (only if empty)
        Position oneForward = new Position(from.row + direction, from.col);
        if (oneForward.isOnBoard() && board.getPieceAt(oneForward) == null) {
            addPawnMove(moves, from, oneForward, pawn, null, promotionRow);

            // 2. Move forward two squares from starting row (only if both squares are empty)
            if (from.row == startRow) {
                Position twoForward = new Position(from.row + 2 * direction, from.col);
                if (board.getPieceAt(twoForward) == null) {
                    moves.add(new Move(from, twoForward, pawn, null, false, false, null));
                }
            }
        }

        // 3. Diagonal captures (only if an enemy piece is there)
        int[] captureCols = {from.col - 1, from.col + 1};
        for (int col : captureCols) {
            Position capturePos = new Position(from.row + direction, col);
            if (!capturePos.isOnBoard()) continue;

            Piece target = board.getPieceAt(capturePos);
            if (target != null && target.getColor() != color) {
                addPawnMove(moves, from, capturePos, pawn, target, promotionRow);
            }
        }

        // 4. En passant capture
        Position enPassantTarget = board.getEnPassantTarget();
        if (enPassantTarget != null && enPassantTarget.row == from.row + direction) {
            if (enPassantTarget.col == from.col - 1 || enPassantTarget.col == from.col + 1) {
                // The captured pawn sits beside our pawn, not on the target square itself
                Position capturedPawnPos = new Position(from.row, enPassantTarget.col);
                Piece capturedPawn = board.getPieceAt(capturedPawnPos);
                if (capturedPawn != null && capturedPawn.getType() == PieceType.PAWN
                        && capturedPawn.getColor() != color) {
                    moves.add(new Move(from, enPassantTarget, pawn, capturedPawn, false, true, null));
                }
            }
        }

        return moves;
    }

    // Adds a pawn move — if it lands on the promotion rank, generates one move
    // per possible promotion piece (Queen/Rook/Bishop/Knight) instead of one plain move.
    private void addPawnMove(List<Move> moves, Position from, Position to, Piece pawn,
                             Piece captured, int promotionRow) {
        if (to.row == promotionRow) {
            for (PieceType choice : PROMOTION_CHOICES) {
                moves.add(new Move(from, to, pawn, captured, false, false, choice));
            }
        } else {
            moves.add(new Move(from, to, pawn, captured, false, false, null));
        }
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

        addCastlingMoves(board, from, king, moves);
        return moves;
    }

    private void addCastlingMoves(Board board, Position kingPos, Piece king, List<Move> moves) {
        Color color = king.getColor();
        int row = kingPos.row;

        // King must not currently be in check to castle at all
        if (isSquareAttacked(board, kingPos, color.opposite())) return;

        // Kingside (short) castling
        if (board.canCastleKingside(color)) {
            Position f = new Position(row, 5);
            Position g = new Position(row, 6);
            if (board.getPieceAt(f) == null && board.getPieceAt(g) == null
                    && !isSquareAttacked(board, f, color.opposite())
                    && !isSquareAttacked(board, g, color.opposite())) {
                moves.add(new Move(kingPos, g, king, null, true, false, null));
            }
        }

        // Queenside (long) castling
        if (board.canCastleQueenside(color)) {
            Position d = new Position(row, 3);
            Position c = new Position(row, 2);
            Position b = new Position(row, 1);
            if (board.getPieceAt(d) == null && board.getPieceAt(c) == null && board.getPieceAt(b) == null
                    && !isSquareAttacked(board, d, color.opposite())
                    && !isSquareAttacked(board, c, color.opposite())) {
                moves.add(new Move(kingPos, c, king, null, true, false, null));
            }
        }
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

    public boolean isSquareAttacked(Board board, Position square, Color attackerColor) {
        // NOTE: uses only non-castling moves for attack detection, since castling
        // itself can never "attack" a square, and to avoid infinite recursion
        // (castling legality depends on isSquareAttacked).
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                Piece piece = board.getPieceAt(pos);
                if (piece == null || piece.getColor() != attackerColor) continue;

                List<Move> moves = (piece.getType() == PieceType.KING)
                        ? generateKingAttackSquaresOnly(board, pos, piece)
                        : generateMovesForPiece(board, pos);

                for (Move move : moves) {
                    if (move.to.equals(square)) return true;
                }
            }
        }
        return false;
    }

    // King's basic 1-square attack pattern, WITHOUT castling — used only for
    // attack-detection to avoid infinite recursion through addCastlingMoves.
    private List<Move> generateKingAttackSquaresOnly(Board board, Position from, Piece king) {
        List<Move> moves = new ArrayList<>();
        for (int[] offset : KING_OFFSETS) {
            Position to = new Position(from.row + offset[0], from.col + offset[1]);
            if (!to.isOnBoard()) continue;
            Piece target = board.getPieceAt(to);
            if (target == null || target.getColor() != king.getColor()) {
                moves.add(new Move(from, to, king, target, false, false, null));
            }
        }
        return moves;
    }

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
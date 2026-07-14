package chess;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class ChessLogicTest {

    @Test
    void startingPositionHas20LegalMovesForWhite() {
        Board board = new Board();
        MoveGenerator generator = new MoveGenerator();

        List<Move> legalMoves = generator.generateLegalMoves(board, Color.WHITE);

        assertEquals(20, legalMoves.size(),
                "White should have exactly 20 legal opening moves");
    }

    @Test
    void startingPositionIsEvaluatedAsBalanced() {
        Board board = new Board();
        Evaluator evaluator = new Evaluator();

        int score = evaluator.evaluate(board);

        assertEquals(0, score,
                "The starting position should be perfectly balanced (material-wise)");
    }

    @Test
    void pinnedPieceCannotMakeIllegalMoves() {
        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));
        board.setPieceAt(new Position(1, 4), new Piece(PieceType.ROOK, Color.WHITE));
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.ROOK, Color.BLACK));
        board.setPieceAt(new Position(7, 0), new Piece(PieceType.KING, Color.BLACK));

        MoveGenerator generator = new MoveGenerator();
        List<Move> legalMoves = generator.generateLegalMoves(board, Color.WHITE);

        long rookMoves = legalMoves.stream()
                .filter(m -> m.from.equals(new Position(1, 4)))
                .count();

        assertEquals(6, rookMoves,
                "A pinned rook should only have legal moves along the pin line");
    }

    @Test
    void backRankCheckmateIsDetectedCorrectly() {
        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }
        board.setPieceAt(new Position(7, 6), new Piece(PieceType.KING, Color.BLACK));
        board.setPieceAt(new Position(6, 5), new Piece(PieceType.PAWN, Color.BLACK));
        board.setPieceAt(new Position(6, 6), new Piece(PieceType.PAWN, Color.BLACK));
        board.setPieceAt(new Position(6, 7), new Piece(PieceType.PAWN, Color.BLACK));
        board.setPieceAt(new Position(0, 0), new Piece(PieceType.ROOK, Color.WHITE));
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));

        MoveGenerator generator = new MoveGenerator();
        Piece rook = board.getPieceAt(new Position(0, 0));
        Move mateMove = new Move(new Position(0, 0), new Position(7, 0), rook, null, false, false, null);
        board.applyMove(mateMove);

        assertTrue(board.isCheckmate(Color.BLACK, generator),
                "Black should be checkmated after Ra8#");
    }

    @Test
    void pawnPromotesToAllFourPieceOptions() {
        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }
        board.setPieceAt(new Position(6, 0), new Piece(PieceType.PAWN, Color.WHITE));
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.KING, Color.BLACK));

        MoveGenerator generator = new MoveGenerator();
        List<Move> pawnMoves = generator.generatePawnMoves(board, new Position(6, 0));

        long promotionOptions = pawnMoves.stream()
                .filter(m -> m.promotionType != null)
                .count();

        assertEquals(4, promotionOptions,
                "A pawn reaching the final rank should have 4 promotion choices");
    }

    @Test
    void castlingMovesTheRookCorrectly() {
        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));
        board.setPieceAt(new Position(0, 7), new Piece(PieceType.ROOK, Color.WHITE));
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.KING, Color.BLACK));

        MoveGenerator generator = new MoveGenerator();
        List<Move> kingMoves = generator.generateKingMoves(board, new Position(0, 4));

        Move kingsideCastle = kingMoves.stream()
                .filter(m -> m.isCastling && m.to.col == 6)
                .findFirst()
                .orElse(null);

        assertNotNull(kingsideCastle, "Kingside castling should be a legal move here");

        board.applyMove(kingsideCastle);

        Piece kingAtG1 = board.getPieceAt(new Position(0, 6));
        Piece rookAtF1 = board.getPieceAt(new Position(0, 5));

        assertNotNull(kingAtG1, "King should have moved to g1");
        assertEquals(PieceType.KING, kingAtG1.getType());
        assertNotNull(rookAtF1, "Rook should have moved to f1");
        assertEquals(PieceType.ROOK, rookAtF1.getType());
    }

    @Test
    void enPassantCapturesTheCorrectPawn() {
        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }
        board.setPieceAt(new Position(4, 4), new Piece(PieceType.PAWN, Color.WHITE));
        board.setPieceAt(new Position(6, 3), new Piece(PieceType.PAWN, Color.BLACK));
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.KING, Color.BLACK));

        Piece blackPawn = board.getPieceAt(new Position(6, 3));
        Move push = new Move(new Position(6, 3), new Position(4, 3), blackPawn, null, false, false, null);
        board.applyMove(push);

        MoveGenerator generator = new MoveGenerator();
        List<Move> whitePawnMoves = generator.generatePawnMoves(board, new Position(4, 4));

        Move enPassant = whitePawnMoves.stream()
                .filter(m -> m.isEnPassant)
                .findFirst()
                .orElse(null);

        assertNotNull(enPassant, "En passant capture should be available immediately after the two-square push");

        board.applyMove(enPassant);

        assertNull(board.getPieceAt(new Position(4, 3)), "Captured Black pawn should be removed from d5");
        Piece whitePawnNewSquare = board.getPieceAt(new Position(5, 3));
        assertNotNull(whitePawnNewSquare, "White pawn should have landed on d6");
        assertEquals(PieceType.PAWN, whitePawnNewSquare.getType());
    }
}
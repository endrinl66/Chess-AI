package org.example;

import chess.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        testCastling();
        System.out.println("\n----------------------------------------\n");
        testEnPassant();
    }

    private static void testCastling() {
        System.out.println("=== TEST: Castling ===");

        Board board = new Board();
        // Clear the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }

        // White king on e1, rooks on a1 and h1, nothing in between — both
        // castling options should be legal. Add a Black king so the board
        // is valid for check detection.
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));  // e1
        board.setPieceAt(new Position(0, 0), new Piece(PieceType.ROOK, Color.WHITE));  // a1
        board.setPieceAt(new Position(0, 7), new Piece(PieceType.ROOK, Color.WHITE));  // h1
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.KING, Color.BLACK));  // e8

        System.out.println("Position before castling:");
        board.printBoard();

        MoveGenerator generator = new MoveGenerator();
        List<Move> kingMoves = generator.generateKingMoves(board, new Position(0, 4));

        System.out.println("\nKing's available moves (looking for castling moves):");
        boolean foundKingside = false;
        boolean foundQueenside = false;
        for (Move m : kingMoves) {
            String note = m.isCastling ? " [CASTLING]" : "";
            System.out.println("  -> (" + m.to.row + "," + m.to.col + ")" + note);
            if (m.isCastling && m.to.col == 6) foundKingside = true;
            if (m.isCastling && m.to.col == 2) foundQueenside = true;
        }

        System.out.println("\nKingside castling generated: " + foundKingside);
        System.out.println("Queenside castling generated: " + foundQueenside);

        // Apply kingside castling and verify both king AND rook moved correctly
        Move kingsideCastle = null;
        for (Move m : kingMoves) {
            if (m.isCastling && m.to.col == 6) kingsideCastle = m;
        }

        if (kingsideCastle != null) {
            board.applyMove(kingsideCastle);
            System.out.println("\nPosition after kingside castling (O-O):");
            board.printBoard();

            Piece kingAtG1 = board.getPieceAt(new Position(0, 6));
            Piece rookAtF1 = board.getPieceAt(new Position(0, 5));
            System.out.println("\nKing correctly on g1: " + (kingAtG1 != null && kingAtG1.getType() == PieceType.KING));
            System.out.println("Rook correctly on f1: " + (rookAtF1 != null && rookAtF1.getType() == PieceType.ROOK));
        } else {
            System.out.println("\nERROR: No kingside castling move was found to test.");
        }
    }

    private static void testEnPassant() {
        System.out.println("=== TEST: En Passant ===");

        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }

        // White pawn on e5 (row 4), Black pawn on d7 (row 6) about to push
        // two squares to d5 (row 4) — landing beside the White pawn and
        // becoming capturable en passant.
        board.setPieceAt(new Position(4, 4), new Piece(PieceType.PAWN, Color.WHITE));  // e5
        board.setPieceAt(new Position(6, 3), new Piece(PieceType.PAWN, Color.BLACK));  // d7
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));  // e1
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.KING, Color.BLACK));  // e8

        System.out.println("Position before Black's two-square pawn push:");
        board.printBoard();

        MoveGenerator generator = new MoveGenerator();

        // Simulate Black pushing d7-d5 (two squares)
        Position from = new Position(6, 3);
        Position to = new Position(4, 3);
        Piece blackPawn = board.getPieceAt(from);
        Move blackPush = new Move(from, to, blackPawn, null, false, false, null);
        board.applyMove(blackPush);

        System.out.println("\nPosition after Black plays d7-d5:");
        board.printBoard();
        System.out.println("En passant target square set: " + board.getEnPassantTarget());

        // Now check White's pawn on e5 for an en passant capture option
        List<Move> whitePawnMoves = generator.generatePawnMoves(board, new Position(4, 4));

        System.out.println("\nWhite pawn's available moves (looking for en passant):");
        boolean foundEnPassant = false;
        Move enPassantMove = null;
        for (Move m : whitePawnMoves) {
            String note = m.isEnPassant ? " [EN PASSANT]" : "";
            System.out.println("  -> (" + m.to.row + "," + m.to.col + ")" + note);
            if (m.isEnPassant) {
                foundEnPassant = true;
                enPassantMove = m;
            }
        }

        System.out.println("\nEn passant capture generated: " + foundEnPassant);

        if (enPassantMove != null) {
            board.applyMove(enPassantMove);
            System.out.println("\nPosition after White captures en passant (exd6):");
            board.printBoard();

            Piece capturedPawnSquare = board.getPieceAt(new Position(4, 3)); // where Black pawn was
            Piece whitePawnNewSquare = board.getPieceAt(new Position(5, 3)); // d6

            System.out.println("\nBlack pawn correctly removed from d5: " + (capturedPawnSquare == null));
            System.out.println("White pawn correctly landed on d6: "
                    + (whitePawnNewSquare != null && whitePawnNewSquare.getType() == PieceType.PAWN));
        } else {
            System.out.println("\nERROR: No en passant move was found to test.");
        }
    }
}
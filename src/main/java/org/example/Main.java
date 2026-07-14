package org.example;

import chess.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();

        // Clear the board and set up a pawn one step from promoting
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.setPieceAt(new Position(row, col), null);
            }
        }
        board.setPieceAt(new Position(6, 0), new Piece(PieceType.PAWN, Color.WHITE));  // a7
        board.setPieceAt(new Position(0, 4), new Piece(PieceType.KING, Color.WHITE));  // e1
        board.setPieceAt(new Position(7, 4), new Piece(PieceType.KING, Color.BLACK));  // e8

        System.out.println("Before promotion:");
        board.printBoard();

        MoveGenerator generator = new MoveGenerator();
        List<Move> pawnMoves = generator.generatePawnMoves(board, new Position(6, 0));

        System.out.println("\nPromotion moves generated for pawn on a7:");
        for (Move m : pawnMoves) {
            System.out.println("  -> (" + m.to.row + "," + m.to.col + ") promotes to " + m.promotionType);
        }

        // Apply the Queen promotion specifically
        Move queenPromotion = pawnMoves.stream()
                .filter(m -> m.promotionType == PieceType.QUEEN)
                .findFirst().orElseThrow();
        board.applyMove(queenPromotion);

        System.out.println("\nAfter promoting to Queen:");
        board.printBoard();
    }
}
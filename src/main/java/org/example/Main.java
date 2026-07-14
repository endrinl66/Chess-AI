package org.example;

import chess.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        board.printBoard();

        ChessAI ai = new ChessAI();
        int searchDepth = 3;

        System.out.println("\nAI is analyzing (depth " + searchDepth + ")...");

        long startTime = System.currentTimeMillis();
        ai.resetNodeCount();
        List<MoveSuggestion> topMoves = ai.getTopMoves(board, searchDepth, 3);
        long endTime = System.currentTimeMillis();

        System.out.println("\nTop 3 suggested moves:");
        int rank = 1;
        for (MoveSuggestion suggestion : topMoves) {
            Move m = suggestion.move;
            System.out.printf("%d. (%d,%d) -> (%d,%d)  eval=%d  winProb=%.1f%%%n",
                    rank++, m.from.row, m.from.col, m.to.row, m.to.col,
                    suggestion.evalScore, suggestion.winProbability * 100);
        }

        System.out.println("\nPositions evaluated: " + ai.getNodesEvaluated());
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
    }
}
package chess;

import java.util.ArrayList;
import java.util.List;

public class ChessAI {
    private final Evaluator evaluator = new Evaluator();
    private final MoveGenerator moveGenerator = new MoveGenerator();

    private int nodesEvaluated = 0;

    public int getNodesEvaluated() {
        return nodesEvaluated;
    }

    public void resetNodeCount() {
        nodesEvaluated = 0;
    }

    public int minimax(Board board, int depth, int alpha, int beta, boolean maximizing) {
        if (depth == 0) {
            nodesEvaluated++;
            return evaluator.evaluate(board);
        }

        Color currentColor = maximizing ? Color.WHITE : Color.BLACK;
        List<Move> moves = moveGenerator.generateLegalMoves(board, currentColor);

        if (moves.isEmpty()) {
            nodesEvaluated++;
            boolean inCheck = board.isInCheck(currentColor, moveGenerator);
            if (inCheck) {
                return maximizing ? (Integer.MIN_VALUE + 1000 - depth) : (Integer.MAX_VALUE - 1000 + depth);
            } else {
                return 0;
            }
        }

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                board.applyMove(move);
                int eval = minimax(board, depth - 1, alpha, beta, false);
                board.undoMove(move);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                board.applyMove(move);
                int eval = minimax(board, depth - 1, alpha, beta, true);
                board.undoMove(move);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    public Move findBestMove(Board board, int depth) {
        List<MoveSuggestion> ranked = getTopMoves(board, depth, 1);
        return ranked.isEmpty() ? null : ranked.get(0).move;
    }

    // Evaluates every legal move and returns the top N, ranked best-first,
    // each with an estimated win probability.
    public List<MoveSuggestion> getTopMoves(Board board, int depth, int topN) {
        Color color = board.getTurn();
        boolean whiteToMove = (color == Color.WHITE);
        List<Move> legalMoves = moveGenerator.generateLegalMoves(board, color);
        List<MoveSuggestion> results = new ArrayList<>();

        for (Move move : legalMoves) {
            board.applyMove(move);
            int eval = minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, !whiteToMove);
            board.undoMove(move);

            double winProb = WinProbability.fromEval(eval, whiteToMove);
            results.add(new MoveSuggestion(move, eval, winProb));
        }

        // Sort best-first: highest eval favors White, lowest favors Black
        results.sort((a, b) -> whiteToMove
                ? Integer.compare(b.evalScore, a.evalScore)
                : Integer.compare(a.evalScore, b.evalScore));

        return results.subList(0, Math.min(topN, results.size()));
    }
}
package chess;

public class MoveSuggestion {
    public final Move move;
    public final int evalScore;
    public final double winProbability;

    public MoveSuggestion(Move move, int evalScore, double winProbability) {
        this.move = move;
        this.evalScore = evalScore;
        this.winProbability = winProbability;
    }
}
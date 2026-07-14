package chess;

public class WinProbability {
    // Converts an evaluation score (in centipawns, where 100 = 1 pawn advantage)
    // into an estimated win probability using a logistic function — similar in
    // spirit to what sites like Lichess use for their analysis bars.
    //
    // IMPORTANT: this is a heuristic mapping of our own evaluation score, not a
    // statistically calibrated probability based on real game outcomes. Worth
    // stating clearly in the README rather than implying it's more rigorous
    // than it is.
    public static double fromEval(int evalCentipawns, boolean forWhite) {
        double pawns = evalCentipawns / 100.0;
        double probForWhite = 1.0 / (1.0 + Math.pow(10, -pawns / 4.0));
        return forWhite ? probForWhite : 1.0 - probForWhite;
    }
}
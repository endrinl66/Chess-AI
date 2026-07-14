# Chess AI

A chess engine and AI opponent, written from scratch in Java. No chess
libraries, no external engines — move generation, check/checkmate detection,
the evaluation function, and the minimax search are all hand-built.

![Board](screenshots/board.png)

## What it does

You play White against an AI opponent. Before each of your moves, a panel
shows the AI's top 3 candidate moves for the current position along with an
estimated win probability for each. You can adjust the AI's difficulty
(Easy/Medium/Hard) from the top bar, which changes how many moves ahead it
searches.

## Running it

Requires Java 17+.

```
git clone https://github.com/endrinl66/Chess-AI.git
cd Chess-AI
mvn compile
mvn exec:java -Dexec.mainClass="chess.ChessGUI"
```

Or download the JAR from the Releases page and run it directly:

```
java -jar Chess-AI-1.0-SNAPSHOT.jar
```

## How the AI works

The engine generates every legal move for a position (accounting for pins,
so a piece can't move if doing so would expose its own king to check), then
scores positions using material count plus piece-square tables — small
bonuses or penalties for where a piece stands on the board, e.g. knights are
worth more centralized.

Move selection uses minimax search with alpha-beta pruning: it looks several
moves ahead assuming both sides play their best available move, and prunes
branches that can't possibly change the outcome. This is why deeper search
(higher difficulty) takes measurably longer — the status bar shows exactly
how many milliseconds the AI's last move took.

The win-probability numbers shown for each suggested move come from a
logistic transform of the evaluation score. It's worth being clear that
this is a heuristic based on the engine's own scoring, not a statistically
calibrated probability from real game data — it's meant to give an
intuitive sense of "how good is this move," not a precise prediction.

## Project structure

```
src/main/java/chess/
  Color.java           WHITE / BLACK
  PieceType.java        the six piece types
  Piece.java             a piece: type + color
  Position.java           a board square
  Move.java                a move, including castling/en passant/promotion flags
  Board.java                 board state, turn tracking, apply/undo move logic
  MoveGenerator.java          legal move generation and check detection
  Evaluator.java                position scoring
  ChessAI.java                   minimax search
  WinProbability.java             eval score to win %
  MoveSuggestion.java              a candidate move with its score
  ChessGUI.java                     the Swing interface
```

I built and tested each piece incrementally rather than writing the whole
engine at once — move generation was checked against known-correct results
(20 legal opening moves from the starting position, a hand-built pin
scenario, a scripted checkmate) before the AI search was added on top.

## Testing

```
mvn test
```

Seven tests cover the parts most likely to have subtle bugs: legal move
count from the starting position, pin handling, checkmate detection,
castling (verifying both the king and rook move correctly), en passant, and
pawn promotion.

## What's not implemented

- No draw offers, resignation, or move history/undo during actual play
  (undo exists internally for the AI's own search, just not exposed to the
  player)
- Difficulty only controls search depth — there's no time-based thinking
  limit
- Pieces are rendered as styled Unicode chess symbols, not custom artwork

## Possible next steps

- Move ordering, so alpha-beta pruning is more effective at higher depth
- An opening book
- Move history in algebraic notation with undo/redo

## Built with

Java 21, Maven, Swing, JUnit 5.

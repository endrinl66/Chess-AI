# Chess AI — Parlour Edition

A chess engine and GUI built entirely from scratch in Java — no chess libraries,
no external engines. Play against an AI opponent powered by minimax with
alpha-beta pruning, and see its top 3 candidate moves with estimated win
probabilities before you move, styled after a 19th-century parlour chess set.

![Chess AI screenshot](screenshots/board.png)

## Features

- **Full legal move generation** for all six piece types, including pins,
  checks, castling (both sides), en passant, and pawn promotion
- **Checkmate and stalemate detection**
- **AI opponent** using minimax search with alpha-beta pruning and a
  positional evaluation function (material + piece-square tables)
- **"Advisor's Counsel" panel** — the AI's top 3 candidate moves for the
  current position, each with an estimated win probability
- **Custom GUI** (Java Swing) with animated piece movement, a responsive
  board that scales with the window, a pawn-promotion choice dialog, and a
  game-over overlay
- **7 passing JUnit tests** covering move generation, pins, checkmate
  detection, castling, en passant, and promotion

## How to run it

### Option 1 — Run the packaged JAR (no setup required)
Download `Chess-AI-1.0-SNAPSHOT.jar` from the [Releases](../../releases) page
(or from `target/` if building from source) and run:

```
java -jar Chess-AI-1.0-SNAPSHOT.jar
```

Requires Java 17 or newer installed.

### Option 2 — Run from source
```
git clone https://github.com/<your-username>/Chess-AI.git
cd Chess-AI
mvn compile
mvn exec:java -Dexec.mainClass="chess.ChessGUI"
```

Or open the project in IntelliJ IDEA, let Maven sync, and run
`ChessGUI.main()` directly.

### Running the tests
```
mvn test
```

## Architecture

```
src/main/java/chess/
  Color.java          — enum: WHITE, BLACK
  PieceType.java       — enum: PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
  Piece.java            — a piece: type + color
  Position.java          — a board square (row, col)
  Move.java                — a move: from, to, captured piece, castling/
                              en passant/promotion flags
  Board.java                 — 8x8 grid, turn tracking, castling rights,
                                en passant target, apply/undo move logic
  MoveGenerator.java          — legal move generation for every piece type,
                                 check detection, castling/en passant rules
  Evaluator.java                — position scoring: material + piece-square
                                   tables
  ChessAI.java                    — minimax search with alpha-beta pruning,
                                     move ranking
  WinProbability.java               — converts an evaluation score into an
                                       estimated win percentage
  MoveSuggestion.java                — a candidate move + its score + win %
  ChessGUI.java                       — the Swing UI: board rendering,
                                         animation, input handling, the
                                         suggestion panel
```

Each class was built and verified incrementally — move generation was tested
against known correct results (20 legal opening moves for White; a hand-built
pinned-piece position; a scripted back-rank checkmate) before the AI was
layered on top of it.

## How the AI works

1. **Move generation** produces every legal move for the side to move,
   filtering out any move that would leave the mover's own king in check
   (this is what correctly handles pins).
2. **Evaluation** scores a position from White's perspective: positive means
   White is better, negative means Black is better. The score combines raw
   material count with piece-square tables — positional bonuses/penalties
   based on where each piece stands (e.g., knights are stronger centralized).
3. **Minimax with alpha-beta pruning** searches several moves ahead,
   assuming both sides play their best available move at each step.
   Alpha-beta pruning skips branches that can't possibly affect the final
   decision, which measurably reduces the number of positions the engine has
   to evaluate at a given search depth.
4. **Move ranking** runs this search across every legal move in the current
   position (not just the best one), so the top 3 can be shown with their
   respective scores.
5. **Win probability** is a logistic transform of the evaluation score,
   similar in spirit to how sites like Lichess present an evaluation bar as
   a percentage.

**A note on honesty**: the win-probability figure is a heuristic derived from
this engine's own evaluation score — it is *not* a statistically calibrated
probability based on real game outcomes (which would require training against
a large dataset of completed games). It's a useful, intuitive way to present
"how good is this move" at a glance, not a rigorous prediction.

## Known limitations

- The knight's move generation, evaluation, and search are all fully general,
  but the GUI's piece rendering uses standard Unicode chess glyphs rather
  than custom artwork.
- There is currently no support for offering/accepting a draw, resigning, or
  move history / undo during play (though `Board.undoMove()` exists
  internally and is used by the AI's search).
- The AI's search depth is fixed; there's no time-based "think longer on hard
  positions" logic.

## Possible future improvements

- Move ordering (searching likely-good moves first) to make alpha-beta
  pruning more effective at greater depth
- An opening book for stronger, faster early-game play
- Adjustable difficulty (exposed search depth, or a simple/strong toggle)
- Move history with algebraic notation and the ability to undo/redo during
  play

## Tech stack

- Java 21, Maven
- Java Swing for the GUI (no external UI frameworks)
- JUnit 5 for testing

## License

This project is available for personal and educational use.

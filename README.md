<div align="center">

# ♟️ Chess AI — Parlour Edition

**A complete chess engine and AI opponent, built entirely from scratch in Java.**

No chess libraries. No external engines. Every rule, every algorithm,
every pixel of the board — hand-built and tested.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-red?logo=apachemaven)
![Tests](https://img.shields.io/badge/Tests-7%20passing-brightgreen)
![License](https://img.shields.io/badge/License-Educational%20Use-blue)

![Chess AI screenshot](screenshots/board.png)

</div>

---

## 🚀 Quick Start

```bash
git clone https://github.com/endrinl66/Chess-AI.git
cd Chess-AI
mvn compile
mvn exec:java -Dexec.mainClass="chess.ChessGUI"
```

Or grab the pre-built JAR from [Releases](../../releases) and run:

```bash
java -jar Chess-AI-1.0-SNAPSHOT.jar
```

Requires **Java 17+**.

---

## 📋 Table of Contents

- [What This Project Demonstrates](#-what-this-project-demonstrates)
- [Features](#-features)
- [Architecture](#-architecture)
- [How the AI Works](#-how-the-ai-works)
- [Difficulty Levels](#-difficulty-levels)
- [Testing](#-testing)
- [Known Limitations](#-known-limitations)
- [Future Improvements](#-future-improvements)
- [Tech Stack](#-tech-stack)

---

## 🎯 What This Project Demonstrates

- **Real algorithms, not glue code** — minimax with alpha-beta pruning,
  implemented from first principles, not a wrapped library
- **Rigorous, incremental verification** — every layer (move generation,
  check detection, castling, en passant) was built and confirmed correct
  against known results before the next layer was added
- **Full-stack Java skills** — from low-level game logic to a custom-painted
  Swing GUI with animation, responsive layout, and interactive controls
- **Honest engineering communication** — this README states plainly what's
  a genuine algorithmic result vs. a UX heuristic (see the win-probability
  note below)

---

## ✨ Features

| Feature | Description |
|---|---|
| ♟️ **Full legal move generation** | All six piece types — pins, checks, castling (both sides), en passant, pawn promotion |
| 👑 **Checkmate & stalemate detection** | The game correctly recognizes and announces the end of a match |
| 🧠 **AI opponent** | Minimax search + alpha-beta pruning, evaluating positions via material + piece-square tables |
| 🎚️ **Adjustable difficulty** | Easy / Medium / Hard — switch live from the top bar |
| 💡 **"Advisor's Counsel" panel** | Top 3 candidate moves for the current position, each with an estimated win probability |
| 🎨 **Fully custom GUI** | Animated piece movement, a board that scales with the window, promotion dialog, game-over overlay |
| ⏱️ **Live performance feedback** | Status bar shows exactly how long the AI took to "think" on its last move |
| ✅ **7 passing JUnit tests** | Move generation, pins, checkmate, castling, en passant, promotion |

---

## 🏗️ Architecture

```
src/main/java/chess/
├── Color.java             enum — WHITE, BLACK
├── PieceType.java          enum — PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
├── Piece.java               a piece: type + color
├── Position.java             a board square (row, col)
├── Move.java                   a move: from, to, captured piece,
│                                castling / en passant / promotion flags
├── Board.java                   8x8 grid, turn tracking, castling rights,
│                                 en passant target, apply/undo move logic
├── MoveGenerator.java           legal move generation, check detection,
│                                 castling & en passant rules
├── Evaluator.java                position scoring — material + piece-
│                                  square tables
├── ChessAI.java                   minimax + alpha-beta pruning, move
│                                   ranking
├── WinProbability.java             eval score → estimated win %
├── MoveSuggestion.java              a candidate move + score + win %
└── ChessGUI.java                     the Swing UI — rendering, animation,
                                       input, difficulty selector, panel
```

Each class was built and verified **incrementally**: move generation was
tested against known-correct results — 20 legal opening moves for White, a
hand-built pinned-piece position, a scripted back-rank checkmate — before
the AI was ever layered on top.

---

## 🧠 How the AI Works

1. **Move generation** produces every legal move for the side to move,
   filtering out any move that would leave the mover's own king in check —
   correctly handling pins.
2. **Evaluation** scores a position from White's perspective (positive =
   White is better) by combining material count with piece-square tables —
   positional bonuses based on where each piece stands.
3. **Minimax with alpha-beta pruning** searches several moves ahead,
   assuming both sides play their best move at each step. Alpha-beta
   pruning skips branches that can't affect the final decision, measurably
   reducing positions evaluated at a given depth.
4. **Move ranking** runs this search across *every* legal move (not just
   the best one), so the top 3 can be shown together with their scores.
5. **Win probability** is a logistic transform of the evaluation score —
   similar in spirit to how Lichess presents its evaluation bar as a
   percentage.

> **Honesty note:** the win-probability figure is a heuristic derived from
> this engine's own evaluation score — it is *not* a statistically
> calibrated probability from real game outcomes (which would require
> training on a large dataset of completed games). It's an intuitive way to
> present "how good is this move," not a rigorous prediction.

---

## 🎚️ Difficulty Levels

| Level | Search depth |
|:---|:---:|
| Easy | 2 plies |
| Medium | 3 plies |
| Hard | 4 plies |

Minimax's cost grows quickly with depth, so higher difficulty means both
stronger play *and* longer "thinking" time — visible directly via the
on-screen millisecond timer, not just claimed here.

---

## ✅ Testing

```bash
mvn test
```

**7 JUnit tests**, targeting the logic most likely to hide subtle bugs:

- ✔️ Starting position produces exactly 20 legal moves for White
- ✔️ Starting position evaluates as perfectly balanced
- ✔️ A pinned piece cannot make an illegal move
- ✔️ Back-rank checkmate is detected correctly
- ✔️ A pawn on the final rank offers all 4 promotion choices
- ✔️ Castling correctly moves both the king and rook
- ✔️ En passant captures the correct pawn and clears the correct square

---

## ⚠️ Known Limitations

- Pieces render as styled Unicode chess glyphs rather than fully custom
  vector artwork
- No draw offers, resignation, or move history/undo during play (though
  `Board.undoMove()` exists internally for the AI's search)
- Search depth is the only difficulty lever — no time-based "think longer"
  logic, and suggestion quality always matches the AI's own move quality

---

## 🔭 Future Improvements

- **Move ordering** for more effective pruning at greater depth
- **Opening book** for stronger, faster early-game play
- **Move history** in algebraic notation, with undo/redo

---

## 🛠️ Tech Stack

`Java 21` · `Maven` · `Java Swing` · `JUnit 5`

---

<div align="center">

*This project is available for personal and educational use.*

</div>

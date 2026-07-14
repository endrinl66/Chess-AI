package chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class ChessGUI extends JPanel {
    private static final int MARGIN = 46;
    private static final int SIDE_PANEL_WIDTH = 260;
    private static final int AI_SEARCH_DEPTH = 3;
    private static final int MIN_SQUARE_SIZE = 30;
    private static final int MAX_SQUARE_SIZE = 110;
    private static final int ANIMATION_STEPS = 24;
    private static final int ANIMATION_DELAY_MS = 16;

    private final Board board = new Board();
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final ChessAI ai = new ChessAI();

    private Position selectedSquare = null;
    private List<Move> legalMovesForSelected = null;
    private String statusText = "Your move (White)";
    private boolean aiThinking = false;

    private List<MoveSuggestion> currentSuggestions = null;
    private boolean suggestionsLoading = false;

    private int squareSize = 80;
    private int boardPixels = squareSize * 8;
    private int boardOriginX = MARGIN;
    private int boardOriginY = MARGIN + 28;

    private boolean animating = false;
    private Move animatingMove = null;
    private double animationProgress = 0.0;
    private Timer animationTimer = null;

    private int moveCount = 0;

    // Game-over overlay state
    private boolean gameOver = false;
    private String gameOverTitle = "";
    private String gameOverDetail = "";

    private static final String WHITE_SYMBOLS = "\u2659\u2658\u2657\u2656\u2655\u2654";
    private static final String BLACK_SYMBOLS = "\u265F\u265E\u265D\u265C\u265B\u265A";
    private static final String[] FILE_LETTERS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    private static final java.awt.Color LIGHT_SQUARE = new java.awt.Color(238, 223, 194);
    private static final java.awt.Color DARK_SQUARE = new java.awt.Color(107, 66, 38);
    private static final java.awt.Color FRAME_WOOD_LIGHT = new java.awt.Color(120, 74, 42);
    private static final java.awt.Color FRAME_WOOD_DARK = new java.awt.Color(58, 34, 18);
    private static final java.awt.Color BRASS = new java.awt.Color(200, 162, 84);
    private static final java.awt.Color SELECT_HIGHLIGHT = new java.awt.Color(200, 162, 84, 140);
    private static final java.awt.Color MOVE_DOT = new java.awt.Color(46, 87, 56, 200);
    private static final java.awt.Color BACKDROP = new java.awt.Color(26, 20, 16);
    private static final java.awt.Color PARCHMENT = new java.awt.Color(233, 219, 188);
    private static final java.awt.Color PARCHMENT_EDGE = new java.awt.Color(150, 120, 80);

    public ChessGUI() {
        setPreferredSize(new Dimension(80 * 8 + MARGIN * 2 + SIDE_PANEL_WIDTH, 80 * 8 + MARGIN * 2 + 40));
        setMinimumSize(new Dimension(500, 400));
        setBackground(BACKDROP);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                computeLayout();
                handleClick(e.getX() - boardOriginX, e.getY() - boardOriginY);
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaint();
            }
        });

        refreshSuggestions();
    }

    private void computeLayout() {
        int totalW = getWidth();
        int totalH = getHeight();
        int availableW = totalW - MARGIN * 2 - SIDE_PANEL_WIDTH;
        int availableH = totalH - MARGIN * 2 - 40;
        int boardSize = Math.max(MIN_SQUARE_SIZE * 8, Math.min(availableW, availableH));

        squareSize = Math.max(MIN_SQUARE_SIZE, Math.min(MAX_SQUARE_SIZE, boardSize / 8));
        boardPixels = squareSize * 8;
        boardOriginX = MARGIN;
        boardOriginY = MARGIN + 28;
    }

    private void handleClick(int x, int y) {
        if (gameOver) return;
        if (animating) return;
        if (aiThinking) return;
        if (board.getTurn() != Color.WHITE) return;
        if (x < 0 || x >= boardPixels || y < 0 || y >= boardPixels) return;

        int col = x / squareSize;
        int row = 7 - (y / squareSize);
        Position clicked = new Position(row, col);
        if (!clicked.isOnBoard()) return;

        if (selectedSquare == null) {
            Piece piece = board.getPieceAt(clicked);
            if (piece != null && piece.getColor() == board.getTurn()) {
                selectedSquare = clicked;
                legalMovesForSelected = filterMovesFrom(
                        moveGenerator.generateLegalMoves(board, board.getTurn()), clicked);
            }
        } else {
            Move chosenMove = findMoveTo(legalMovesForSelected, clicked);
            selectedSquare = null;
            legalMovesForSelected = null;

            if (chosenMove != null) {
                Move moveToPlay = chosenMove;
                animateMove(moveToPlay, () -> {
                    board.applyMove(moveToPlay);
                    board.switchTurn();
                    moveCount++;
                    currentSuggestions = null;
                    repaint();
                    checkGameEndOrTriggerAI();
                });
                return;
            }
        }
        repaint();
    }

    private void animateMove(Move move, Runnable onComplete) {
        animating = true;
        animatingMove = move;
        animationProgress = 0.0;

        animationTimer = new Timer(ANIMATION_DELAY_MS, null);
        animationTimer.addActionListener(e -> {
            animationProgress += 1.0 / ANIMATION_STEPS;
            if (animationProgress >= 1.0) {
                animationProgress = 1.0;
                animationTimer.stop();
                animating = false;
                animatingMove = null;
                onComplete.run();
            } else {
                repaint();
            }
        });
        animationTimer.start();
    }

    private void checkGameEndOrTriggerAI() {
        if (board.isCheckmate(Color.BLACK, moveGenerator)) {
            declareGameOver("Checkmate!", "White triumphs in " + moveCount + " moves.");
            return;
        }
        if (board.isStalemate(Color.BLACK, moveGenerator)) {
            declareGameOver("Stalemate", "The game ends in a draw after " + moveCount + " moves.");
            return;
        }

        statusText = "Black (AI) is thinking...";
        aiThinking = true;
        repaint();

        SwingWorker<Move, Void> worker = new SwingWorker<>() {
            @Override
            protected Move doInBackground() {
                return ai.findBestMove(board.copy(), AI_SEARCH_DEPTH);
            }

            @Override
            protected void done() {
                Move aiMove = null;
                try {
                    aiMove = get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (aiMove == null) {
                    aiThinking = false;
                    repaint();
                    return;
                }

                Move finalAiMove = aiMove;
                animateMove(finalAiMove, () -> {
                    board.applyMove(finalAiMove);
                    board.switchTurn();
                    moveCount++;
                    aiThinking = false;

                    if (board.isCheckmate(Color.WHITE, moveGenerator)) {
                        declareGameOver("Checkmate!", "Black (AI) triumphs in " + moveCount + " moves.");
                    } else if (board.isStalemate(Color.WHITE, moveGenerator)) {
                        declareGameOver("Stalemate", "The game ends in a draw after " + moveCount + " moves.");
                    } else {
                        statusText = "Your move (White)";
                        refreshSuggestions();
                    }
                    repaint();
                });
            }
        };
        worker.execute();
    }

    private void declareGameOver(String title, String detail) {
        gameOver = true;
        gameOverTitle = title;
        gameOverDetail = detail;
        statusText = title;
        repaint();
    }

    private void refreshSuggestions() {
        if (gameOver) return;
        if (board.getTurn() != Color.WHITE) return;
        if (board.isCheckmate(Color.WHITE, moveGenerator) || board.isStalemate(Color.WHITE, moveGenerator)) return;

        suggestionsLoading = true;
        currentSuggestions = null;
        repaint();

        SwingWorker<List<MoveSuggestion>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MoveSuggestion> doInBackground() {
                return ai.getTopMoves(board.copy(), AI_SEARCH_DEPTH, 3);
            }

            @Override
            protected void done() {
                try {
                    currentSuggestions = get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                suggestionsLoading = false;
                repaint();
            }
        };
        worker.execute();
    }

    private List<Move> filterMovesFrom(List<Move> moves, Position from) {
        moves.removeIf(m -> !m.from.equals(from));
        return moves;
    }

    private Move findMoveTo(List<Move> moves, Position to) {
        for (Move m : moves) {
            if (m.to.equals(to)) return m;
        }
        return null;
    }

    private String squareName(Position p) {
        return FILE_LETTERS[p.col] + (p.row + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        computeLayout();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(BRASS);
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        g2.drawString(statusText, MARGIN, 22);

        Graphics2D boardG = (Graphics2D) g2.create();
        boardG.translate(boardOriginX, boardOriginY);
        drawWoodFrame(boardG);
        drawSquares(boardG);
        highlightSelection(boardG);
        drawPieces(boardG);
        drawCoordinateLabels(boardG);
        if (gameOver) {
            drawGameOverOverlay(boardG);
        }
        boardG.dispose();

        Graphics2D panelG = (Graphics2D) g2.create();
        panelG.translate(boardOriginX + boardPixels + 24, boardOriginY);
        drawSuggestionPanel(panelG);
        panelG.dispose();
    }

    // Draws a dark scrim over the board plus a centered parchment card
    // announcing the result — like a "Game Over" card on a real table.
    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new java.awt.Color(0, 0, 0, 130));
        g2.fillRect(0, 0, boardPixels, boardPixels);

        int cardWidth = Math.min(boardPixels - 40, 320);
        int cardHeight = 150;
        int cardX = (boardPixels - cardWidth) / 2;
        int cardY = (boardPixels - cardHeight) / 2;

        RoundRectangle2D card = new RoundRectangle2D.Double(cardX, cardY, cardWidth, cardHeight, 14, 14);
        g2.setColor(PARCHMENT);
        g2.fill(card);
        g2.setColor(BRASS);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(card);
        g2.setStroke(new BasicStroke(1f));
        RoundRectangle2D innerLine = new RoundRectangle2D.Double(cardX + 6, cardY + 6, cardWidth - 12, cardHeight - 12, 10, 10);
        g2.draw(innerLine);

        FontMetrics fm;

        g2.setFont(new Font("Serif", Font.BOLD, 15));
        g2.setColor(new java.awt.Color(90, 70, 40));
        String header = "GAME OVER";
        fm = g2.getFontMetrics();
        g2.drawString(header, cardX + (cardWidth - fm.stringWidth(header)) / 2, cardY + 32);

        g2.setFont(new Font("Serif", Font.BOLD, 24));
        g2.setColor(new java.awt.Color(122, 24, 30));
        fm = g2.getFontMetrics();
        g2.drawString(gameOverTitle, cardX + (cardWidth - fm.stringWidth(gameOverTitle)) / 2, cardY + 72);

        g2.setFont(new Font("Serif", Font.ITALIC, 14));
        g2.setColor(new java.awt.Color(58, 34, 18));
        fm = g2.getFontMetrics();
        g2.drawString(gameOverDetail, cardX + (cardWidth - fm.stringWidth(gameOverDetail)) / 2, cardY + 105);

        g2.setFont(new Font("Serif", Font.PLAIN, 12));
        g2.setColor(new java.awt.Color(120, 100, 70));
        String hint = "Close and relaunch to play again";
        fm = g2.getFontMetrics();
        g2.drawString(hint, cardX + (cardWidth - fm.stringWidth(hint)) / 2, cardY + 130);
    }

    private void drawCoordinateLabels(Graphics2D g2) {
        g2.setFont(new Font("Serif", Font.BOLD, Math.max(11, squareSize / 6)));
        g2.setColor(BRASS);

        for (int row = 0; row < 8; row++) {
            int y = (7 - row) * squareSize;
            String label = String.valueOf(row + 1);
            g2.drawString(label, -34, y + squareSize / 2 + 5);
        }

        for (int col = 0; col < 8; col++) {
            int x = col * squareSize;
            String label = FILE_LETTERS[col];
            g2.drawString(label, x + squareSize / 2 - 4, boardPixels + 34);
        }
    }

    private void drawSuggestionPanel(Graphics2D g2) {
        int panelHeight = boardPixels;
        int width = SIDE_PANEL_WIDTH - 24;

        g2.setColor(PARCHMENT);
        g2.fillRoundRect(0, 0, width, panelHeight, 10, 10);
        g2.setColor(PARCHMENT_EDGE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(0, 0, width, panelHeight, 10, 10);

        g2.setColor(new java.awt.Color(58, 34, 18));
        g2.setFont(new Font("Serif", Font.BOLD, 17));
        g2.drawString("Advisor's Counsel", 16, 28);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(16, 38, width - 16, 38);

        g2.setFont(new Font("Serif", Font.PLAIN, 13));
        int y = 62;

        if (gameOver) {
            g2.drawString("The game has concluded.", 16, y);
            return;
        }

        if (suggestionsLoading) {
            g2.drawString("Consulting the tomes...", 16, y);
            return;
        }

        if (currentSuggestions == null || currentSuggestions.isEmpty()) {
            g2.drawString("No counsel available.", 16, y);
            return;
        }

        String[] ordinals = {"First", "Second", "Third"};
        int rank = 0;
        for (MoveSuggestion s : currentSuggestions) {
            String moveText = squareName(s.move.from) + " \u2192 " + squareName(s.move.to);
            String pctText = String.format("%.0f%% favorable", s.winProbability * 100);

            g2.setFont(new Font("Serif", Font.BOLD, 14));
            g2.setColor(new java.awt.Color(122, 24, 30));
            g2.drawString(ordinals[rank] + " counsel:", 16, y);
            y += 20;

            g2.setFont(new Font("Serif", Font.PLAIN, 15));
            g2.setColor(new java.awt.Color(30, 20, 10));
            g2.drawString(moveText, 24, y);
            y += 18;

            g2.setFont(new Font("Serif", Font.ITALIC, 13));
            g2.setColor(new java.awt.Color(90, 70, 40));
            g2.drawString(pctText, 24, y);
            y += 30;

            rank++;
        }
    }

    private void drawWoodFrame(Graphics2D g2) {
        int pad = 16;
        GradientPaint woodGradient = new GradientPaint(
                -pad, -pad, FRAME_WOOD_LIGHT,
                boardPixels + pad, boardPixels + pad, FRAME_WOOD_DARK);
        g2.setPaint(woodGradient);
        g2.fillRect(-pad, -pad, boardPixels + pad * 2, boardPixels + pad * 2);

        g2.setColor(BRASS);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect(-pad + 4, -pad + 4, boardPixels + pad * 2 - 8, boardPixels + pad * 2 - 8);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(-4, -4, boardPixels + 8, boardPixels + 8);
    }

    private void drawSquares(Graphics2D g2) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isLight = (row + col) % 2 == 0;
                int x = col * squareSize;
                int y = (7 - row) * squareSize;

                java.awt.Color base = isLight ? LIGHT_SQUARE : DARK_SQUARE;
                java.awt.Color shade = isLight
                        ? new java.awt.Color(225, 208, 176)
                        : new java.awt.Color(90, 54, 30);
                GradientPaint squareGradient = new GradientPaint(x, y, base, x + squareSize, y + squareSize, shade);
                g2.setPaint(squareGradient);
                g2.fillRect(x, y, squareSize, squareSize);
            }
        }
    }

    private void highlightSelection(Graphics2D g2) {
        if (selectedSquare != null) {
            g2.setColor(SELECT_HIGHLIGHT);
            int x = selectedSquare.col * squareSize;
            int y = (7 - selectedSquare.row) * squareSize;
            g2.fillRect(x, y, squareSize, squareSize);

            if (legalMovesForSelected != null) {
                for (Move m : legalMovesForSelected) {
                    int mx = m.to.col * squareSize;
                    int my = (7 - m.to.row) * squareSize;
                    int r = squareSize / 3;
                    Ellipse2D dot = new Ellipse2D.Double(
                            mx + (squareSize - r) / 2.0, my + (squareSize - r) / 2.0, r, r);
                    g2.setColor(MOVE_DOT);
                    g2.fill(dot);
                }
            }
        }
    }

    private void drawPieces(Graphics2D g2) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);

                if (animating && animatingMove != null && animatingMove.from.equals(pos)) {
                    continue;
                }

                Piece p = board.getPieceAt(pos);
                if (p == null) continue;

                int x = col * squareSize;
                int y = (7 - row) * squareSize;
                drawPieceGlyph(g2, p, x, y);
            }
        }

        if (animating && animatingMove != null) {
            double t = easeInOut(animationProgress);

            int fromX = animatingMove.from.col * squareSize;
            int fromY = (7 - animatingMove.from.row) * squareSize;
            int toX = animatingMove.to.col * squareSize;
            int toY = (7 - animatingMove.to.row) * squareSize;

            int curX = (int) (fromX + (toX - fromX) * t);
            int curY = (int) (fromY + (toY - fromY) * t);

            drawPieceGlyph(g2, animatingMove.movedPiece, curX, curY);
        }
    }

    private double easeInOut(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private void drawPieceGlyph(Graphics2D g2, Piece p, int x, int y) {
        Font pieceFont = new Font("Segoe UI Symbol", Font.PLAIN, Math.max(14, squareSize - 6));
        if (!pieceFont.getFamily().equals("Segoe UI Symbol")) {
            pieceFont = new Font("Serif", Font.BOLD, Math.max(14, squareSize - 10));
        }
        g2.setFont(pieceFont);

        String symbol = (p.getColor() == Color.WHITE)
                ? String.valueOf(WHITE_SYMBOLS.charAt(p.getType().ordinal()))
                : String.valueOf(BLACK_SYMBOLS.charAt(p.getType().ordinal()));

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(symbol);
        int textX = x + (squareSize - textWidth) / 2;
        int textY = y + ((squareSize - fm.getHeight()) / 2) + fm.getAscent();

        g2.setColor(new java.awt.Color(0, 0, 0, 70));
        Ellipse2D shadow = new Ellipse2D.Double(
                x + squareSize * 0.22, y + squareSize * 0.78, squareSize * 0.56, squareSize * 0.14);
        g2.fill(shadow);

        if (p.getColor() == Color.WHITE) {
            GradientPaint ivory = new GradientPaint(
                    textX, textY - fm.getAscent(), new java.awt.Color(255, 250, 235),
                    textX, textY, new java.awt.Color(212, 191, 152));
            drawEmbossedGlyph(g2, symbol, textX, textY, ivory, new java.awt.Color(120, 90, 50));
        } else {
            GradientPaint walnut = new GradientPaint(
                    textX, textY - fm.getAscent(), new java.awt.Color(70, 45, 30),
                    textX, textY, new java.awt.Color(20, 12, 8));
            drawEmbossedGlyph(g2, symbol, textX, textY, walnut, BRASS);
        }
    }

    private void drawEmbossedGlyph(Graphics2D g2, String symbol, int x, int y,
                                   Paint fillPaint, java.awt.Color outlineColor) {
        g2.setColor(outlineColor);
        int[] dx = {-1, 1, -1, 1, 0, 0};
        int[] dy = {-1, -1, 1, 1, -1, 1};
        for (int i = 0; i < dx.length; i++) {
            g2.drawString(symbol, x + dx[i], y + dy[i]);
        }
        g2.setPaint(fillPaint);
        g2.drawString(symbol, x, y);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Chess AI \u2014 Parlour Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BACKDROP);
        frame.add(new ChessGUI());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
    }
}
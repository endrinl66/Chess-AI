package chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.ArrayList;

public class ChessGUI extends JPanel {
    private static final int MARGIN = 50;
    private static final int SIDE_PANEL_WIDTH = 300;
    private static final int AI_SEARCH_DEPTH = 3;
    private static final int MIN_SQUARE_SIZE = 40;
    private static final int MAX_SQUARE_SIZE = 150;
    private static final int DEFAULT_SQUARE_SIZE = 115;
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

    private int squareSize = DEFAULT_SQUARE_SIZE;
    private int boardPixels = squareSize * 8;
    private int boardOriginX = MARGIN;
    private int boardOriginY = MARGIN + 30;

    private boolean animating = false;
    private Move animatingMove = null;
    private double animationProgress = 0.0;
    private Timer animationTimer = null;

    private int moveCount = 0;

    private boolean gameOver = false;
    private String gameOverTitle = "";
    private String gameOverDetail = "";

    private static final String WHITE_SYMBOLS = "\u2659\u2658\u2657\u2656\u2655\u2654"; // P N B R Q K
    private static final String BLACK_SYMBOLS = "\u265F\u265E\u265D\u265C\u265B\u265A"; // p n b r q k
    private static final String[] FILE_LETTERS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    private static final java.awt.Color SQUARE_BLACK_TOP = new java.awt.Color(38, 34, 34);
    private static final java.awt.Color SQUARE_BLACK_BOTTOM = new java.awt.Color(8, 6, 6);
    private static final java.awt.Color SQUARE_RED_TOP = new java.awt.Color(214, 30, 40);
    private static final java.awt.Color SQUARE_RED_BOTTOM = new java.awt.Color(96, 8, 14);
    private static final java.awt.Color FRAME_RED_LIGHT = new java.awt.Color(190, 20, 28);
    private static final java.awt.Color FRAME_RED_DARK = new java.awt.Color(60, 4, 8);
    private static final java.awt.Color FRAME_HIGHLIGHT = new java.awt.Color(255, 140, 130, 170);
    private static final java.awt.Color GOLD_ACCENT = new java.awt.Color(224, 190, 120);
    private static final java.awt.Color SELECT_HIGHLIGHT = new java.awt.Color(255, 210, 90, 150);
    private static final java.awt.Color MOVE_DOT = new java.awt.Color(255, 230, 120, 210);
    private static final java.awt.Color BACKDROP = new java.awt.Color(10, 8, 8);
    private static final java.awt.Color PARCHMENT = new java.awt.Color(24, 18, 18);
    private static final java.awt.Color PARCHMENT_EDGE = new java.awt.Color(190, 20, 28);

    private static final java.awt.Color GLASS_RED_LIGHT = new java.awt.Color(255, 110, 105);
    private static final java.awt.Color GLASS_RED_DARK = new java.awt.Color(110, 6, 10);
    private static final java.awt.Color GLASS_BLACK_LIGHT = new java.awt.Color(80, 76, 76);
    private static final java.awt.Color GLASS_BLACK_DARK = new java.awt.Color(3, 2, 2);
    private static final java.awt.Color GLASS_OUTLINE = new java.awt.Color(0, 0, 0, 200);

    public ChessGUI() {
        setPreferredSize(new Dimension(
                DEFAULT_SQUARE_SIZE * 8 + MARGIN * 2 + SIDE_PANEL_WIDTH,
                DEFAULT_SQUARE_SIZE * 8 + MARGIN * 2 + 44));
        setMinimumSize(new Dimension(600, 480));
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
        int availableH = totalH - MARGIN * 2 - 44;
        int boardSize = Math.max(MIN_SQUARE_SIZE * 8, Math.min(availableW, availableH));

        squareSize = Math.max(MIN_SQUARE_SIZE, Math.min(MAX_SQUARE_SIZE, boardSize / 8));
        boardPixels = squareSize * 8;
        boardOriginX = MARGIN;
        boardOriginY = MARGIN + 30;
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
            Move chosenMove = resolveChosenMove(legalMovesForSelected, clicked);
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

    private Move resolveChosenMove(List<Move> moves, Position to) {
        List<Move> matches = new ArrayList<>();
        for (Move m : moves) {
            if (m.to.equals(to)) matches.add(m);
        }
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        return showPromotionDialog(matches);
    }

    private Move showPromotionDialog(List<Move> promotionMoves) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose a piece for your pawn to become:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice < 0) choice = 0;

        PieceType chosenType = switch (choice) {
            case 1 -> PieceType.ROOK;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.KNIGHT;
            default -> PieceType.QUEEN;
        };

        for (Move m : promotionMoves) {
            if (m.promotionType == chosenType) return m;
        }
        return promotionMoves.get(0);
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

        g2.setColor(GOLD_ACCENT);
        g2.setFont(new Font("Serif", Font.BOLD, 18));
        g2.drawString(statusText, MARGIN, 26);

        Graphics2D boardG = (Graphics2D) g2.create();
        boardG.translate(boardOriginX, boardOriginY);
        drawResinFrame(boardG);
        drawSquares(boardG);
        highlightSelection(boardG);
        drawPieces(boardG);
        drawCoordinateLabels(boardG);
        if (gameOver) {
            drawGameOverOverlay(boardG);
        }
        boardG.dispose();

        Graphics2D panelG = (Graphics2D) g2.create();
        panelG.translate(boardOriginX + boardPixels + 28, boardOriginY);
        drawSuggestionPanel(panelG);
        panelG.dispose();
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new java.awt.Color(0, 0, 0, 150));
        g2.fillRect(0, 0, boardPixels, boardPixels);

        int cardWidth = Math.min(boardPixels - 60, 360);
        int cardHeight = 170;
        int cardX = (boardPixels - cardWidth) / 2;
        int cardY = (boardPixels - cardHeight) / 2;

        RoundRectangle2D card = new RoundRectangle2D.Double(cardX, cardY, cardWidth, cardHeight, 16, 16);
        g2.setColor(new java.awt.Color(20, 14, 14));
        g2.fill(card);
        g2.setColor(FRAME_RED_LIGHT);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(card);
        g2.setStroke(new BasicStroke(1f));
        RoundRectangle2D innerLine = new RoundRectangle2D.Double(cardX + 7, cardY + 7, cardWidth - 14, cardHeight - 14, 12, 12);
        g2.setColor(GOLD_ACCENT);
        g2.draw(innerLine);

        FontMetrics fm;

        g2.setFont(new Font("Serif", Font.BOLD, 16));
        g2.setColor(GOLD_ACCENT);
        String header = "GAME OVER";
        fm = g2.getFontMetrics();
        g2.drawString(header, cardX + (cardWidth - fm.stringWidth(header)) / 2, cardY + 36);

        g2.setFont(new Font("Serif", Font.BOLD, 28));
        g2.setColor(new java.awt.Color(230, 40, 50));
        fm = g2.getFontMetrics();
        g2.drawString(gameOverTitle, cardX + (cardWidth - fm.stringWidth(gameOverTitle)) / 2, cardY + 82);

        g2.setFont(new Font("Serif", Font.ITALIC, 15));
        g2.setColor(new java.awt.Color(220, 210, 210));
        fm = g2.getFontMetrics();
        g2.drawString(gameOverDetail, cardX + (cardWidth - fm.stringWidth(gameOverDetail)) / 2, cardY + 118);

        g2.setFont(new Font("Serif", Font.PLAIN, 13));
        g2.setColor(new java.awt.Color(170, 150, 150));
        String hint = "Close and relaunch to play again";
        fm = g2.getFontMetrics();
        g2.drawString(hint, cardX + (cardWidth - fm.stringWidth(hint)) / 2, cardY + 148);
    }

    private void drawCoordinateLabels(Graphics2D g2) {
        g2.setFont(new Font("Serif", Font.BOLD, Math.max(12, squareSize / 6)));
        g2.setColor(GOLD_ACCENT);

        for (int row = 0; row < 8; row++) {
            int y = (7 - row) * squareSize;
            String label = String.valueOf(row + 1);
            g2.drawString(label, -36, y + squareSize / 2 + 5);
        }

        for (int col = 0; col < 8; col++) {
            int x = col * squareSize;
            String label = FILE_LETTERS[col];
            g2.drawString(label, x + squareSize / 2 - 4, boardPixels + 36);
        }
    }

    // Renders the side panel as if the AI is narrating its own analysis:
    // an intro line describing what it just did, followed by the ranked
    // moves and their win probabilities.
    private void drawSuggestionPanel(Graphics2D g2) {
        int panelHeight = boardPixels;
        int width = SIDE_PANEL_WIDTH - 28;

        g2.setColor(PARCHMENT);
        g2.fillRoundRect(0, 0, width, panelHeight, 12, 12);
        g2.setColor(PARCHMENT_EDGE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(0, 0, width, panelHeight, 12, 12);

        g2.setColor(GOLD_ACCENT);
        g2.setFont(new Font("Serif", Font.BOLD, 19));
        g2.drawString("Advisor's Counsel", 18, 32);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(18, 42, width - 18, 42);

        int y = 64;

        if (gameOver) {
            g2.setFont(new Font("Serif", Font.ITALIC, 14));
            g2.setColor(new java.awt.Color(210, 195, 195));
            g2.drawString("The game has concluded.", 18, y);
            return;
        }

        if (suggestionsLoading) {
            g2.setFont(new Font("Serif", Font.ITALIC, 14));
            g2.setColor(new java.awt.Color(210, 195, 195));
            g2.drawString("Consulting the tomes...", 18, y);
            return;
        }

        if (currentSuggestions == null || currentSuggestions.isEmpty()) {
            g2.setFont(new Font("Serif", Font.ITALIC, 14));
            g2.setColor(new java.awt.Color(210, 195, 195));
            g2.drawString("No counsel available.", 18, y);
            return;
        }

        // Conversational intro, wrapped across two lines to fit the panel width
        g2.setFont(new Font("Serif", Font.ITALIC, 13));
        g2.setColor(new java.awt.Color(210, 195, 195));
        g2.drawString("I analyzed the position and found", 18, y);
        y += 17;
        g2.drawString("the best 3 moves for this turn:", 18, y);
        y += 30;

        String[] ordinals = {"First", "Second", "Third"};
        int rank = 0;
        for (MoveSuggestion s : currentSuggestions) {
            String moveText = squareName(s.move.from) + " \u2192 " + squareName(s.move.to);
            String pctText = String.format("%.0f%% chance of success", s.winProbability * 100);

            g2.setFont(new Font("Serif", Font.BOLD, 15));
            g2.setColor(new java.awt.Color(220, 40, 50));
            g2.drawString(ordinals[rank] + " choice:", 18, y);
            y += 22;

            g2.setFont(new Font("Serif", Font.PLAIN, 16));
            g2.setColor(new java.awt.Color(225, 215, 215));
            g2.drawString(moveText, 26, y);
            y += 19;

            g2.setFont(new Font("Serif", Font.ITALIC, 14));
            g2.setColor(GOLD_ACCENT);
            g2.drawString(pctText, 26, y);
            y += 34;

            rank++;
        }
    }

    private void drawResinFrame(Graphics2D g2) {
        int pad = 26;
        GradientPaint resinGradient = new GradientPaint(
                -pad, -pad, FRAME_RED_LIGHT,
                boardPixels + pad, boardPixels + pad, FRAME_RED_DARK);
        g2.setPaint(resinGradient);
        g2.fillRoundRect(-pad, -pad, boardPixels + pad * 2, boardPixels + pad * 2, 18, 18);

        GradientPaint highlight = new GradientPaint(
                -pad, -pad, FRAME_HIGHLIGHT,
                -pad, boardPixels * 0.3f, new java.awt.Color(255, 140, 130, 0));
        g2.setPaint(highlight);
        g2.fillRoundRect(-pad, -pad, boardPixels + pad * 2, (int) (boardPixels * 0.3), 18, 18);

        g2.setColor(GOLD_ACCENT);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(-pad + 6, -pad + 6, boardPixels + pad * 2 - 12, boardPixels + pad * 2 - 12, 14, 14);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new java.awt.Color(255, 220, 180));
        g2.drawRect(-4, -4, boardPixels + 8, boardPixels + 8);
    }

    private void drawSquares(Graphics2D g2) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isBlack = (row + col) % 2 == 0;
                int x = col * squareSize;
                int y = (7 - row) * squareSize;

                java.awt.Color top = isBlack ? SQUARE_BLACK_TOP : SQUARE_RED_TOP;
                java.awt.Color bottom = isBlack ? SQUARE_BLACK_BOTTOM : SQUARE_RED_BOTTOM;
                GradientPaint squareGradient = new GradientPaint(x, y, top, x + squareSize, y + squareSize, bottom);
                g2.setPaint(squareGradient);
                g2.fillRect(x, y, squareSize, squareSize);

                g2.setColor(new java.awt.Color(255, 255, 255, isBlack ? 25 : 45));
                g2.fillRect(x, y, squareSize, Math.max(2, squareSize / 20));
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
        int s = squareSize;
        Font pieceFont = new Font("Segoe UI Symbol", Font.PLAIN, Math.max(16, (int) (s * 0.82)));
        if (!pieceFont.getFamily().equals("Segoe UI Symbol")) {
            pieceFont = new Font("Serif", Font.BOLD, Math.max(16, (int) (s * 0.78)));
        }

        String symbol = (p.getColor() == Color.WHITE)
                ? String.valueOf(WHITE_SYMBOLS.charAt(p.getType().ordinal()))
                : String.valueOf(BLACK_SYMBOLS.charAt(p.getType().ordinal()));

        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector gv = pieceFont.createGlyphVector(frc, symbol);
        Rectangle2D visualBounds = gv.getVisualBounds();

        double drawX = x + (s - visualBounds.getWidth()) / 2.0 - visualBounds.getX();
        double drawY = y + (s - visualBounds.getHeight()) / 2.0 - visualBounds.getY();

        g2.setColor(new java.awt.Color(0, 0, 0, 100));
        Ellipse2D shadow = new Ellipse2D.Double(
                x + s * 0.22, y + s * 0.84, s * 0.56, s * 0.11);
        g2.fill(shadow);

        float glowCx = (float) (x + s * 0.5);
        float glowCy = (float) (y + s * 0.5);
        float glowR = s * 0.42f;
        java.awt.Color rimColor = (p.getColor() == Color.WHITE)
                ? new java.awt.Color(255, 180, 170, 90)
                : new java.awt.Color(255, 225, 160, 90);
        RadialGradientPaint rimGlow = new RadialGradientPaint(
                new Point2D.Float(glowCx, glowCy), glowR,
                new float[]{0f, 1f},
                new java.awt.Color[]{rimColor, new java.awt.Color(rimColor.getRed(), rimColor.getGreen(), rimColor.getBlue(), 0)}
        );
        g2.setPaint(rimGlow);
        g2.fillOval((int) (glowCx - glowR), (int) (glowCy - glowR), (int) (glowR * 2), (int) (glowR * 2));

        java.awt.Color glassLight = (p.getColor() == Color.WHITE) ? GLASS_RED_LIGHT : GLASS_BLACK_LIGHT;
        java.awt.Color glassDark = (p.getColor() == Color.WHITE) ? GLASS_RED_DARK : GLASS_BLACK_DARK;
        java.awt.Color outlineColor = (p.getColor() == Color.WHITE) ? GLASS_OUTLINE : GOLD_ACCENT;

        GradientPaint glass = new GradientPaint(
                (float) drawX, (float) (drawY + visualBounds.getY()), glassLight,
                (float) drawX, (float) (drawY + visualBounds.getY() + visualBounds.getHeight()), glassDark);

        g2.setFont(pieceFont);

        int outlineReach = (p.getColor() == Color.WHITE) ? 1 : 2;
        g2.setColor(outlineColor);
        for (int ox = -outlineReach; ox <= outlineReach; ox++) {
            for (int oy = -outlineReach; oy <= outlineReach; oy++) {
                if (ox == 0 && oy == 0) continue;
                g2.drawGlyphVector(gv, (float) (drawX + ox), (float) (drawY + oy));
            }
        }

        g2.setPaint(glass);
        g2.drawGlyphVector(gv, (float) drawX, (float) drawY);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Chess AI \u2014 Crimson Glass Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BACKDROP);
        frame.add(new ChessGUI());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
    }
}
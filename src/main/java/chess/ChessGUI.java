package chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class ChessGUI extends JPanel {
    private static final int SQUARE_SIZE = 80;
    private static final int BOARD_PIXELS = SQUARE_SIZE * 8;
    private static final int MARGIN = 30;

    private final Board board = new Board();
    private final MoveGenerator moveGenerator = new MoveGenerator();

    private Position selectedSquare = null;
    private List<Move> legalMovesForSelected = null;

    private static final String WHITE_SYMBOLS = "\u2659\u2658\u2657\u2656\u2655\u2654"; // P N B R Q K
    private static final String BLACK_SYMBOLS = "\u265F\u265E\u265D\u265C\u265B\u265A"; // p n b r q k

    // Victorian parlour palette: mahogany, cream, brass, deep burgundy
    private static final java.awt.Color LIGHT_SQUARE = new java.awt.Color(238, 223, 194); // cream/ivory
    private static final java.awt.Color DARK_SQUARE = new java.awt.Color(107, 66, 38);    // mahogany
    private static final java.awt.Color FRAME_WOOD_LIGHT = new java.awt.Color(120, 74, 42);
    private static final java.awt.Color FRAME_WOOD_DARK = new java.awt.Color(58, 34, 18);
    private static final java.awt.Color BRASS = new java.awt.Color(200, 162, 84);
    private static final java.awt.Color SELECT_HIGHLIGHT = new java.awt.Color(200, 162, 84, 140);  // brass glow
    private static final java.awt.Color MOVE_DOT = new java.awt.Color(122, 24, 30, 190);            // burgundy
    private static final java.awt.Color BACKDROP = new java.awt.Color(26, 20, 16);                  // dark parlour walls

    public ChessGUI() {
        setPreferredSize(new Dimension(BOARD_PIXELS + MARGIN * 2, BOARD_PIXELS + MARGIN * 2));
        setBackground(BACKDROP);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX() - MARGIN, e.getY() - MARGIN);
            }
        });
    }

    private void handleClick(int x, int y) {
        int col = x / SQUARE_SIZE;
        int row = 7 - (y / SQUARE_SIZE);
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
            if (chosenMove != null) {
                board.applyMove(chosenMove);
                board.switchTurn();
            }
            selectedSquare = null;
            legalMovesForSelected = null;
        }
        repaint();
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.translate(MARGIN, MARGIN);

        drawWoodFrame(g2);
        drawSquares(g2);
        highlightSelection(g2);
        drawPieces(g2);
    }

    private void drawWoodFrame(Graphics2D g2) {
        int pad = 16;
        GradientPaint woodGradient = new GradientPaint(
                -pad, -pad, FRAME_WOOD_LIGHT,
                BOARD_PIXELS + pad, BOARD_PIXELS + pad, FRAME_WOOD_DARK);
        g2.setPaint(woodGradient);
        g2.fillRect(-pad, -pad, BOARD_PIXELS + pad * 2, BOARD_PIXELS + pad * 2);

        g2.setColor(BRASS);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect(-pad + 4, -pad + 4, BOARD_PIXELS + pad * 2 - 8, BOARD_PIXELS + pad * 2 - 8);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(-4, -4, BOARD_PIXELS + 8, BOARD_PIXELS + 8);
    }

    private void drawSquares(Graphics2D g2) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isLight = (row + col) % 2 == 0;
                int x = col * SQUARE_SIZE;
                int y = (7 - row) * SQUARE_SIZE;

                java.awt.Color base = isLight ? LIGHT_SQUARE : DARK_SQUARE;
                java.awt.Color shade = isLight
                        ? new java.awt.Color(225, 208, 176)
                        : new java.awt.Color(90, 54, 30);
                GradientPaint squareGradient = new GradientPaint(x, y, base, x + SQUARE_SIZE, y + SQUARE_SIZE, shade);
                g2.setPaint(squareGradient);
                g2.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
            }
        }
    }

    private void highlightSelection(Graphics2D g2) {
        if (selectedSquare != null) {
            g2.setColor(SELECT_HIGHLIGHT);
            int x = selectedSquare.col * SQUARE_SIZE;
            int y = (7 - selectedSquare.row) * SQUARE_SIZE;
            g2.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);

            if (legalMovesForSelected != null) {
                for (Move m : legalMovesForSelected) {
                    int mx = m.to.col * SQUARE_SIZE;
                    int my = (7 - m.to.row) * SQUARE_SIZE;
                    int r = SQUARE_SIZE / 3;
                    Ellipse2D dot = new Ellipse2D.Double(
                            mx + (SQUARE_SIZE - r) / 2.0, my + (SQUARE_SIZE - r) / 2.0, r, r);
                    g2.setColor(MOVE_DOT);
                    g2.fill(dot);
                }
            }
        }
    }

    private void drawPieces(Graphics2D g2) {
        Font pieceFont = new Font("Segoe UI Symbol", Font.PLAIN, SQUARE_SIZE - 6);
        if (!pieceFont.getFamily().equals("Segoe UI Symbol")) {
            // Fallback if that font isn't available on this system
            pieceFont = new Font("Serif", Font.BOLD, SQUARE_SIZE - 10);
        }
        g2.setFont(pieceFont);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = board.getPieceAt(new Position(row, col));
                if (p == null) continue;

                String symbol = (p.getColor() == Color.WHITE)
                        ? String.valueOf(WHITE_SYMBOLS.charAt(p.getType().ordinal()))
                        : String.valueOf(BLACK_SYMBOLS.charAt(p.getType().ordinal()));

                int x = col * SQUARE_SIZE;
                int y = (7 - row) * SQUARE_SIZE;

                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(symbol);
                int textX = x + (SQUARE_SIZE - textWidth) / 2;
                int textY = y + ((SQUARE_SIZE - fm.getHeight()) / 2) + fm.getAscent();

                // Soft ground shadow for a sense of weight/depth
                g2.setColor(new java.awt.Color(0, 0, 0, 70));
                Ellipse2D shadow = new Ellipse2D.Double(
                        x + SQUARE_SIZE * 0.22, y + SQUARE_SIZE * 0.78, SQUARE_SIZE * 0.56, SQUARE_SIZE * 0.14);
                g2.fill(shadow);

                if (p.getColor() == Color.WHITE) {
                    // Ivory piece: warm gradient fill, brass-toned outline
                    GradientPaint ivory = new GradientPaint(
                            textX, textY - fm.getAscent(), new java.awt.Color(255, 250, 235),
                            textX, textY, new java.awt.Color(212, 191, 152));
                    drawEmbossedGlyph(g2, symbol, textX, textY, ivory, new java.awt.Color(120, 90, 50));
                } else {
                    // Dark walnut piece: deep gradient fill, brass rim light
                    GradientPaint walnut = new GradientPaint(
                            textX, textY - fm.getAscent(), new java.awt.Color(70, 45, 30),
                            textX, textY, new java.awt.Color(20, 12, 8));
                    drawEmbossedGlyph(g2, symbol, textX, textY, walnut, BRASS);
                }
            }
        }
    }

    // Draws a glyph with a subtle outline (for definition) and a gradient fill
    // (for a carved, three-dimensional look) instead of a flat single color.
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
        frame.setVisible(true);
    }
}
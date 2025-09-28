import java.util.ArrayList;

public final class BitboardMoveGen {

    public static ArrayList<Zug> generate(boolean white, Bitboards bb) {
        ArrayList<Zug> moves = new ArrayList<>();
        long usOcc = white ? bb.occW : bb.occB;
        long themOcc = white ? bb.occB : bb.occW;
        long allOcc = bb.occ;

        // Pawns
        long pawns = white ? bb.w[0] : bb.b[0];
        while (pawns != 0) {
            int from = Long.numberOfTrailingZeros(pawns);
            pawns &= pawns - 1;
            int fx = Bitboards.xOf(from), fy = Bitboards.yOf(from);

            if (white) {
                int to = from - 8;
                if (to >= 0 && ((allOcc >>> to) & 1L) == 0) {
                    addPawnOrPromo(moves, from, to, white);
                    // Double push from rank 6 (fy == 6)
                    if (fy == 6) {
                        int to2 = from - 16;
                        if (((allOcc >>> to2) & 1L) == 0) {
                            moves.add(new Zug(Bitboards.xOf(from), Bitboards.yOf(from), Bitboards.xOf(to2), Bitboards.yOf(to2)));
                        }
                    }
                }
                // Captures
                if (fx > 0) {
                    int cap = from - 9;
                    if (cap >= 0 && (((themOcc >>> cap) & 1L) != 0 || cap == bb.epSquare)) {
                        addPawnOrPromo(moves, from, cap, white);
                    }
                }
                if (fx < 7) {
                    int cap = from - 7;
                    if (cap >= 0 && (((themOcc >>> cap) & 1L) != 0 || cap == bb.epSquare)) {
                        addPawnOrPromo(moves, from, cap, white);
                    }
                }
            } else {
                int to = from + 8;
                if (to < 64 && ((allOcc >>> to) & 1L) == 0) {
                    addPawnOrPromo(moves, from, to, white);
                    // Double push from rank 1 (fy == 1)
                    if (fy == 1) {
                        int to2 = from + 16;
                        if (((allOcc >>> to2) & 1L) == 0) {
                            moves.add(new Zug(Bitboards.xOf(from), Bitboards.yOf(from), Bitboards.xOf(to2), Bitboards.yOf(to2)));
                        }
                    }
                }
                // Captures
                if (fx > 0) {
                    int cap = from + 7;
                    if (cap < 64 && (((themOcc >>> cap) & 1L) != 0 || cap == bb.epSquare)) {
                        addPawnOrPromo(moves, from, cap, white);
                    }
                }
                if (fx < 7) {
                    int cap = from + 9;
                    if (cap < 64 && (((themOcc >>> cap) & 1L) != 0 || cap == bb.epSquare)) {
                        addPawnOrPromo(moves, from, cap, white);
                    }
                }
            }
        }

        // Knights
        long knights = white ? bb.w[1] : bb.b[1];
        addLeaperMoves(moves, knights, bb, Attacks.knight, usOcc);
        // Kings
        long kings = white ? bb.w[5] : bb.b[5];
        addLeaperMoves(moves, kings, bb, Attacks.king, usOcc);
        // Castling (pseudo-legal: empty squares only; check filtered later)
        if (white) {
            // King at e1
            if (bb.wK) {
                int f1 = Bitboards.sq(5,7), g1 = Bitboards.sq(6,7);
                if ((((allOcc >>> f1) & 1L) == 0) && (((allOcc >>> g1) & 1L) == 0)) {
                    moves.add(new Zug("e1g1"));
                }
            }
            if (bb.wQ) {
                int d1 = Bitboards.sq(3,7), c1 = Bitboards.sq(2,7), b1 = Bitboards.sq(1,7);
                if ((((allOcc >>> d1) & 1L) == 0) && (((allOcc >>> c1) & 1L) == 0) && (((allOcc >>> b1) & 1L) == 0)) {
                    moves.add(new Zug("e1c1"));
                }
            }
        } else {
            // Black
            if (bb.bK) {
                int f8 = Bitboards.sq(5,0), g8 = Bitboards.sq(6,0);
                if ((((allOcc >>> f8) & 1L) == 0) && (((allOcc >>> g8) & 1L) == 0)) {
                    moves.add(new Zug("e8g8"));
                }
            }
            if (bb.bQ) {
                int d8 = Bitboards.sq(3,0), c8 = Bitboards.sq(2,0), b8 = Bitboards.sq(1,0);
                if ((((allOcc >>> d8) & 1L) == 0) && (((allOcc >>> c8) & 1L) == 0) && (((allOcc >>> b8) & 1L) == 0)) {
                    moves.add(new Zug("e8c8"));
                }
            }
        }

        // Bishops
        long bishops = white ? bb.w[2] : bb.b[2];
        addSliderMoves(moves, bishops, bb, true, false, usOcc);
        // Rooks
        long rooks = white ? bb.w[3] : bb.b[3];
        addSliderMoves(moves, rooks, bb, false, true, usOcc);
        // Queens
        long queens = white ? bb.w[4] : bb.b[4];
        addSliderMoves(moves, queens, bb, true, true, usOcc);

        return moves;
    }

    private static void addPawnOrPromo(ArrayList<Zug> moves, int from, int to, boolean white) {
        int ty = Bitboards.yOf(to);
        int fx = Bitboards.xOf(from), fy = Bitboards.yOf(from), tx = Bitboards.xOf(to);
        if ((white && ty == 0) || (!white && ty == 7)) {
            moves.add(new Zug(fx, fy, tx, ty, 'q'));
            moves.add(new Zug(fx, fy, tx, ty, 'r'));
            moves.add(new Zug(fx, fy, tx, ty, 'b'));
            moves.add(new Zug(fx, fy, tx, ty, 'n'));
        } else {
            moves.add(new Zug(fx, fy, tx, ty));
        }
    }

    private static void addLeaperMoves(ArrayList<Zug> moves, long pieces, Bitboards bb, long[] attackTable, long usOcc) {
        while (pieces != 0) {
            int from = Long.numberOfTrailingZeros(pieces);
            pieces &= pieces - 1;
            long targets = attackTable[from] & ~usOcc;
            while (targets != 0) {
                int to = Long.numberOfTrailingZeros(targets);
                targets &= targets - 1;
                moves.add(new Zug(Bitboards.xOf(from), Bitboards.yOf(from), Bitboards.xOf(to), Bitboards.yOf(to)));
            }
        }
    }

    private static void addSliderMoves(ArrayList<Zug> moves, long pieces, Bitboards bb, boolean diag, boolean ortho, long usOcc) {
        while (pieces != 0) {
            int from = Long.numberOfTrailingZeros(pieces);
            pieces &= pieces - 1;
            long targets = 0L;
            if (diag) targets |= Attacks.bishopAttacks(from, bb.occ);
            if (ortho) targets |= Attacks.rookAttacks(from, bb.occ);
            targets &= ~usOcc;
            while (targets != 0) {
                int to = Long.numberOfTrailingZeros(targets);
                targets &= targets - 1;
                moves.add(new Zug(Bitboards.xOf(from), Bitboards.yOf(from), Bitboards.xOf(to), Bitboards.yOf(to)));
            }
        }
    }
}

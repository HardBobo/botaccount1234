public final class Attacks {
    // Precomputed attack masks
    public static final long[] knight = new long[64];
    public static final long[] king = new long[64];
    // pawnAttack[color][targetSq] = bitboard of squares from which a pawn of 'color' attacks targetSq
    // color: 0 = white, 1 = black
    public static final long[][] pawnAttack = new long[2][64];

    static {
        init();
    }

    public static void init() {
        // Clear
        for (int i = 0; i < 64; i++) {
            knight[i] = 0L; king[i] = 0L; pawnAttack[0][i] = 0L; pawnAttack[1][i] = 0L;
        }
        // Precompute for each target square
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int s = y * 8 + x;
                // Knight
                int[][] kOff = {{1,2},{2,1},{-1,2},{-2,1},{1,-2},{2,-1},{-1,-2},{-2,-1}};
                long n = 0L;
                for (int[] o : kOff) {
                    int nx = x + o[0], ny = y + o[1];
                    if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) n |= (1L << (ny*8 + nx));
                }
                knight[s] = n;
                // King
                long k = 0L;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) k |= (1L << (ny*8 + nx));
                    }
                }
                king[s] = k;
                // Pawn attackers for target square (reverse attacks)
                // White pawns attack from (x-1,y+1) and (x+1,y+1)
                if (y + 1 < 8) {
                    if (x - 1 >= 0) pawnAttack[0][s] |= (1L << ((y+1)*8 + (x-1)));
                    if (x + 1 < 8) pawnAttack[0][s] |= (1L << ((y+1)*8 + (x+1)));
                }
                // Black pawns attack from (x-1,y-1) and (x+1,y-1)
                if (y - 1 >= 0) {
                    if (x - 1 >= 0) pawnAttack[1][s] |= (1L << ((y-1)*8 + (x-1)));
                    if (x + 1 < 8) pawnAttack[1][s] |= (1L << ((y-1)*8 + (x+1)));
                }
            }
        }
    }

    public static long bishopAttacks(int fromSq, long occ) {
        int fx = fromSq & 7; int fy = fromSq >>> 3;
        long attacks = 0L;
        // NW
        for (int x = fx-1, y = fy-1; x >= 0 && y >= 0; x--, y--) {
            int s = y*8 + x; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break;
        }
        // NE
        for (int x = fx+1, y = fy-1; x < 8 && y >= 0; x++, y--) {
            int s = y*8 + x; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break;
        }
        // SW
        for (int x = fx-1, y = fy+1; x >= 0 && y < 8; x--, y++) {
            int s = y*8 + x; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break;
        }
        // SE
        for (int x = fx+1, y = fy+1; x < 8 && y < 8; x++, y++) {
            int s = y*8 + x; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break;
        }
        return attacks;
    }

    public static long rookAttacks(int fromSq, long occ) {
        int fx = fromSq & 7; int fy = fromSq >>> 3;
        long attacks = 0L;
        // North (y-1..0)
        for (int y = fy-1; y >= 0; y--) { int s = y*8 + fx; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break; }
        // South (y+1..7)
        for (int y = fy+1; y < 8; y++) { int s = y*8 + fx; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break; }
        // West (x-1..0)
        for (int x = fx-1; x >= 0; x--) { int s = fy*8 + x; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break; }
        // East (x+1..7)
        for (int x = fx+1; x < 8; x++) { int s = fy*8 + x; attacks |= (1L << s); if (((occ >>> s) & 1L) != 0) break; }
        return attacks;
    }
}

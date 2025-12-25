import java.util.Arrays;

/**
 * Fixed-size transposition table.
 *
 * Design notes:
 * - Uses clusters (buckets) of multiple entries, similar to top engines.
 * - Replacement is based on a simple depth-vs-age score, inspired by Stockfish's
 *   "depth minus 8 * age" style heuristic.
 * - Stores best move in a compact int encoding to avoid allocating millions of Zug objects.
 */
public final class TranspositionTable {

    // Keep a small cluster size to reduce collisions without hurting locality too much.
    public static final int CLUSTER_SIZE = 4;

    // Approx bytes per entry (arrays only, object headers not counted):
    // key(8) + value(4) + depth(2) + flag(1) + gen(1) + move(4) = 20 bytes
    private static final int BYTES_PER_ENTRY_APPROX = 20;

    private int clusterCount;

    private long[] keys;
    private int[] values;
    private short[] depths;
    private byte[] flags;
    private int[] moves;

    /**
     * Generation byte. 0 is reserved to mean "empty slot".
     * We advance the generation each root search so older entries can be replaced.
     */
    private byte generation = 1;
    private byte[] gens;

    public TranspositionTable(int sizeMb) {
        resizeMB(sizeMb);
    }

    public int sizeMB() {
        // Approximate; not exact accounting of JVM overhead.
        int entries = (keys == null) ? 0 : keys.length;
        long bytes = (long) entries * (long) BYTES_PER_ENTRY_APPROX;
        return (int) (bytes / (1024L * 1024L));
    }

    public int capacityEntries() {
        return keys == null ? 0 : keys.length;
    }

    public void resizeMB(int sizeMb) {
        if (sizeMb <= 0) {
            // Minimal table (still functional).
            sizeMb = 1;
        }

        long sizeBytes = (long) sizeMb * 1024L * 1024L;
        long entries = sizeBytes / BYTES_PER_ENTRY_APPROX;
        long clusters = entries / CLUSTER_SIZE;
        if (clusters < 1) clusters = 1;

        // Keep the requested size as closely as possible (no power-of-two rounding).
        int c = (clusters > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) clusters;
        if (c < 1) c = 1;

        // Ensure arrays fit into Java's int-indexed arrays.
        long totalEntriesL = (long) c * (long) CLUSTER_SIZE;
        if (totalEntriesL > Integer.MAX_VALUE) {
            c = Integer.MAX_VALUE / CLUSTER_SIZE;
            totalEntriesL = (long) c * (long) CLUSTER_SIZE;
        }

        this.clusterCount = c;

        int totalEntries = (int) totalEntriesL;
        keys = new long[totalEntries];
        values = new int[totalEntries];
        depths = new short[totalEntries];
        flags = new byte[totalEntries];
        moves = new int[totalEntries];
        gens = new byte[totalEntries];

        clear();
    }

    public void clear() {
        // gens==0 means "empty"; keys can be left as-is.
        if (gens != null) {
            Arrays.fill(gens, (byte) 0);
        }
        generation = 1;
    }

    /** Call once per root search (i.e., once per move). */
    public void newSearch() {
        int g = (generation & 0xFF) + 1;
        if (g > 255) g = 1; // keep 0 reserved for empty
        generation = (byte) g;
    }

    /**
     * Probe for a key. Returns the slot index, or -1 on miss.
     * On a hit, refreshes the entry generation to make it "younger".
     */
    public int probe(long key) {
        int base = baseIndex(key);
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            int idx = base + i;
            byte g = gens[idx];
            if (g != 0 && keys[idx] == key) {
                gens[idx] = generation; // refresh age on use
                return idx;
            }
        }
        return -1;
    }

    public int valueAt(int idx) {
        return values[idx];
    }

    public int depthAt(int idx) {
        return depths[idx] & 0xFFFF;
    }

    public int flagAt(int idx) {
        return flags[idx];
    }

    public int moveAt(int idx) {
        return moves[idx];
    }

    /**
     * Store/replace an entry.
     *
     * Replacement heuristic:
     * - If same key exists in cluster, update it (depth-prefer + exact-prefer).
     * - Else replace an empty slot if available.
     * - Else replace the entry with the lowest (depth - 8*age) score.
     */
    public void store(long key, int depth, int flag, int value, int bestMoveCode) {
        int base = baseIndex(key);

        // 1) Update if key already present
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            int idx = base + i;
            if (gens[idx] != 0 && keys[idx] == key) {
                // Refresh
                gens[idx] = generation;

                int oldDepth = depths[idx] & 0xFFFF;
                int oldFlag = flags[idx];

                // Keep deeper info unless the new info is exact.
                // This is deliberately simple; top engines use slightly more nuanced rules.
                if (depth >= oldDepth || flag == 0 /* EXACT */) {
                    values[idx] = value;
                    depths[idx] = (short) depth;
                    flags[idx] = (byte) flag;
                } else {
                    // If we don't overwrite value/depth, still allow EXACT to upgrade bounds.
                    if (oldFlag != 0 && flag == 0) {
                        values[idx] = value;
                        depths[idx] = (short) depth;
                        flags[idx] = (byte) flag;
                    }
                }

                if (bestMoveCode != 0) {
                    moves[idx] = bestMoveCode;
                }
                return;
            }
        }

        // 2) Use an empty slot if available
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            int idx = base + i;
            if (gens[idx] == 0) {
                keys[idx] = key;
                values[idx] = value;
                depths[idx] = (short) depth;
                flags[idx] = (byte) flag;
                moves[idx] = bestMoveCode;
                gens[idx] = generation;
                return;
            }
        }

        // 3) Choose a victim by lowest (depth - 8*age)
        int victim = base;
        int worstScore = Integer.MAX_VALUE;
        int gNow = generation & 0xFF;
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            int idx = base + i;
            int d = depths[idx] & 0xFFFF;
            int age = (gNow - (gens[idx] & 0xFF)) & 0xFF;
            int score = d - (age * 8);
            if (score < worstScore) {
                worstScore = score;
                victim = idx;
            }
        }

        keys[victim] = key;
        values[victim] = value;
        depths[victim] = (short) depth;
        flags[victim] = (byte) flag;
        moves[victim] = bestMoveCode;
        gens[victim] = generation;
    }

    private int baseIndex(long key) {
        // Mix to avoid relying only on low bits (though Zobrist keys are already well-distributed).
        // Map a 32-bit hash to [0, clusterCount) using a multiply-high technique (fast, no modulo).
        int h = (int) (key ^ (key >>> 32));
        long uh = Integer.toUnsignedLong(h);
        int cluster = (int) ((uh * (long) clusterCount) >>> 32);
        return cluster * CLUSTER_SIZE;
    }

    // --- Move encoding helpers ---

    /**
     * Encodes a move as: from(6) | to(6)<<6 | promo(3)<<12
     * promo: 0=none, 1=q,2=r,3=b,4=n
     */
    public static int encodeMove(Zug z) {
        if (z == null) return 0;
        int from = z.startY * 8 + z.startX;
        int to = z.endY * 8 + z.endX;
        int promo = switch (z.promoteTo) {
            case 'q' -> 1;
            case 'r' -> 2;
            case 'b' -> 3;
            case 'n' -> 4;
            default -> 0;
        };
        return (from & 63) | ((to & 63) << 6) | ((promo & 7) << 12);
    }

    public static char promoCharFromCode(int promo) {
        return switch (promo) {
            case 1 -> 'q';
            case 2 -> 'r';
            case 3 -> 'b';
            case 4 -> 'n';
            default -> 0;
        };
    }
}

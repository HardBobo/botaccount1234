import java.lang.reflect.Field;

public class DeltaPruningBench {
    private static long getLongStatic(Class<?> cls, String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f.getLong(null);
    }
    private static void setLongStatic(Class<?> cls, String name, long value) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        f.setLong(null, value);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java DeltaPruningBench \"FEN string\"");
            System.out.println("Example: java DeltaPruningBench \"r1n1n1b1/1P1P1P1P/1N1N1N2/2RnQrRq/2pKp3/3BNQbQ/k7/4Bq2\"");
            return;
        }
        String fen = args[0];

        // Setup position
        Board.loadFEN(fen);
        boolean whiteToMove = Board.whiteToMove; // Board.fenToBoard sets this
        Zobrist.initZobrist();
        long hash = Zobrist.computeHash(Board.bitboards, whiteToMove);

        // Reset counters
        setLongStatic(MoveFinder.class, "nodeCount", 0L);
        setLongStatic(MoveFinder.class, "deltaPruneCount", 0L);
        setLongStatic(MoveFinder.class, "deltaConsideredCount", 0L);

        // Run a fixed-depth search (depth 5)
        int depth = 5;
        long start = System.currentTimeMillis();
        int score = MoveFinder.negamax(null, depth, Integer.MIN_VALUE / 2 + 1, Integer.MAX_VALUE / 2 - 1, whiteToMove, hash);
        long end = System.currentTimeMillis();

        long nodes = getLongStatic(MoveFinder.class, "nodeCount");
        long deltaPrunes = getLongStatic(MoveFinder.class, "deltaPruneCount");
        long deltaConsidered = getLongStatic(MoveFinder.class, "deltaConsideredCount");
        long ms = Math.max(1, end - start);
        long nps = (nodes * 1000L) / ms;
        double pct = (deltaConsidered > 0) ? (deltaPrunes * 100.0 / deltaConsidered) : 0.0;

        System.out.println("Delta Pruning Benchmark");
        System.out.println("FEN: " + fen);
        System.out.println("Depth: " + depth);
        System.out.println("Score: " + score);
        System.out.println("Time: " + ms + " ms");
        System.out.println("Nodes: " + nodes);
        System.out.println("NPS: " + nps);
        System.out.printf("Delta prunes: %d / %d (%.2f%%)\n", deltaPrunes, deltaConsidered, pct);
    }
}

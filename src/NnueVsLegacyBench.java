import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks NNUE vs legacy PST evaluation on speed and basic accuracy.
 *
 * Accuracy here is defined on a small labeled set as sign correctness relative to side-to-move
 * (positive=winning for STM, zero=drawish/unclear, negative=losing for STM).
 *
 * Usage:
 *   javac src\\*.java && java -cp src NnueVsLegacyBench
 */
public final class NnueVsLegacyBench {
    private static final int WARMUP_ITERS = 2000;
    private static final int SPEED_ITERS = 10000;

    private static final class LabeledFen {
        final String name;
        final String fen; // plain FEN (no pipes)
        final int expectedSign; // -1 losing for STM, 0 drawish, +1 winning for STM
        LabeledFen(String name, String fen, int expectedSign) {
            this.name = name; this.fen = fen; this.expectedSign = expectedSign;
        }
    }

    public static void main(String[] args) {
        // Try to load NNUE (optional). If not available, NNUE speed/acc will be skipped gracefully.
        try { Nnue.tryAutoLoad(); } catch (Throwable ignored) {}

        List<LabeledFen> tests = new ArrayList<>();
        // Start position: roughly balanced (0)
        tests.add(new LabeledFen("Startpos w", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 0));
        tests.add(new LabeledFen("Startpos b", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1", 0));
        // KQ vs k: winning for side with queen
        tests.add(new LabeledFen("KQ vs k (w)", "4k3/8/8/8/8/8/3Q4/4K3 w - - 0 1", +1));
        tests.add(new LabeledFen("KQ vs k (b)", "4k3/8/8/8/8/8/3Q4/4K3 b - - 0 1", -1));
        // KR vs k: winning for rook side
        tests.add(new LabeledFen("KR vs k (w)", "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1", +1));
        tests.add(new LabeledFen("KR vs k (b)", "4k3/8/8/8/8/8/4R3/4K3 b - - 0 1", -1));
        // Bare kings: drawn
        tests.add(new LabeledFen("K vs k (w)", "4k3/8/8/8/8/8/8/4K3 w - - 0 1", 0));
        tests.add(new LabeledFen("K vs k (b)", "4k3/8/8/8/8/8/8/4K3 b - - 0 1", 0));

        // Speed benchmark FEN pool (include above plus duplicates for volume)
        List<String> speedFens = new ArrayList<>();
        for (LabeledFen lf : tests) speedFens.add(lf.fen);
        // Add a few more random-ish simple positions (no labels needed for speed)
        speedFens.add("r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 2 4");
        speedFens.add("rnbq1rk1/ppp1bppp/3p1n2/4p3/2B1P3/2N2N2/PPPP1PPP/R1BQ1RK1 w - - 5 7");
        speedFens.add("r1bqk2r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 6 5");

        // Accuracy comparison
        System.out.println("=== Accuracy (sign) ===");
        int nnueCorrect = 0, pstCorrect = 0, total = tests.size();
        int nnueAvailable = Nnue.isUsable() ? 1 : 0;
        for (LabeledFen lf : tests) {
            Board.loadFEN(lf.fen);
            int expected = lf.expectedSign;

            int evalPst = Evaluation.evaluatePstOnly(Board.whiteToMove);
            int evalNnue = 0;
            if (nnueAvailable == 1) {
                try { evalNnue = Nnue.evaluate(Board.whiteToMove); } catch (Throwable t) { nnueAvailable = 0; }
            }

            int signPst = cpToSign(evalPst);
            int signNnue = cpToSign(evalNnue);
            boolean pstOk = (signPst == expected);
            boolean nnueOk = (nnueAvailable == 1) && (signNnue == expected);

            if (pstOk) pstCorrect++;
            if (nnueOk) nnueCorrect++;

            System.out.printf("%-20s  expected=%2d  PST=%6d (%2d)  %s", lf.name, expected, evalPst, signPst, pstOk?"OK":"  ");
            if (nnueAvailable == 1) {
                System.out.printf("   NNUE=%6d (%2d)  %s", evalNnue, signNnue, nnueOk?"OK":"  ");
            } else {
                System.out.print("   NNUE=unavailable");
            }
            System.out.println();
        }
        System.out.printf("PST sign accuracy:  %d/%d\n", pstCorrect, total);
        if (nnueAvailable == 1) System.out.printf("NNUE sign accuracy: %d/%d\n", nnueCorrect, total);

        // Speed benchmark (positions per second)
        System.out.println();
        System.out.println("=== Speed (positions/sec) ===");
        // Warmup
        for (int i = 0; i < WARMUP_ITERS; i++) {
            String fen = speedFens.get(i % speedFens.size());
            Board.loadFEN(fen);
            Evaluation.evaluatePstOnly(Board.whiteToMove);
            if (nnueAvailable == 1) { Nnue.evaluate(Board.whiteToMove); }
        }
        // PST timing
        long start = System.nanoTime();
        for (int i = 0; i < SPEED_ITERS; i++) {
            String fen = speedFens.get(i % speedFens.size());
            Board.loadFEN(fen);
            Evaluation.evaluatePstOnly(Board.whiteToMove);
        }
        long pstNanos = System.nanoTime() - start;
        double pstPosPerSec = SPEED_ITERS / (pstNanos / 1e9);

        // NNUE timing (if available)
        double nnuePosPerSec = -1;
        if (nnueAvailable == 1) {
            start = System.nanoTime();
            for (int i = 0; i < SPEED_ITERS; i++) {
                String fen = speedFens.get(i % speedFens.size());
                Board.loadFEN(fen);
                Nnue.evaluate(Board.whiteToMove);
            }
            long nnueNanos = System.nanoTime() - start;
            nnuePosPerSec = SPEED_ITERS / (nnueNanos / 1e9);
        }

        System.out.printf("PST:  %,d iters in %d ms  ->  %.0f pos/sec\n",
                SPEED_ITERS, TimeUnit.NANOSECONDS.toMillis(pstNanos), pstPosPerSec);
        if (nnueAvailable == 1) {
            System.out.printf("NNUE: %,d iters in %d ms  ->  %.0f pos/sec\n",
                    SPEED_ITERS, TimeUnit.NANOSECONDS.toMillis((long)(SPEED_ITERS / nnuePosPerSec * 1e9)), nnuePosPerSec);
        } else {
            System.out.println("NNUE: unavailable");
        }
    }

    private static int cpToSign(int cp) {
        // Small tolerance band around zero to account for noise
        if (cp > 50) return +1;
        if (cp < -50) return -1;
        return 0;
    }
}

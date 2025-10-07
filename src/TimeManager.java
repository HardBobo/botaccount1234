import java.util.ArrayList;

public class TimeManager {
    public static long computeThinkTimeMs(long timeLeftMs, long incMs) {
        // Base suggestion
        long base = (timeLeftMs / 25) + (long)(incMs * 0.7);
        // Soft cap: never spend more than 20% of remaining time in one move
        long softCap = Math.max(200, timeLeftMs / 5);
        long think = Math.min(base, softCap);
        // Emergency floor
        return Math.max(think, 100);
    }
}

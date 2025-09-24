import java.util.ArrayList;

public class TimeManager {
    public static long computeThinkTimeMs(long timeLeftMs, long incMs) {
        return Math.max((long) (timeLeftMs/20 + incMs * 0.8), 100);
    }
}
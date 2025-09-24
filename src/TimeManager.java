import java.util.ArrayList;

public class TimeManager {
    public static long computeThinkTimeMs(long timeLeftMs, long incMs) {
        if (incMs != 0) {
            return Math.max((long) (timeLeftMs/30 + incMs * 0.7), 100);
        } else {
            return timeLeftMs/35;
        }
    }
}
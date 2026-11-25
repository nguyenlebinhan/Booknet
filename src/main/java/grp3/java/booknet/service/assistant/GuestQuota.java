package grp3.java.booknet.service.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Daily AI question limit for guests, counted per IP address so clearing cookies does not reset it.
 * Counts live in memory only: they reset at midnight (Hanoi time) and when the application restarts.
 */
@Component
public class GuestQuota {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final int limit;
    private final Clock clock;
    private final Map<String, Integer> used = new HashMap<>();
    private LocalDate day;

    public GuestQuota(@Value("${app.assistant.guest-daily-limit:5}") int limit, Clock clock) {
        this.limit = Math.max(0, limit);
        this.clock = clock;
    }

    /** Takes one question from today's allowance. False when none is left. */
    public synchronized boolean tryUse(String address) {
        startNewDay();
        int count = used.getOrDefault(address, 0);
        if (count >= limit) {
            return false;
        }
        used.put(address, count + 1);
        return true;
    }

    /** Gives a question back when the AI could not answer it. */
    public synchronized void giveBack(String address) {
        startNewDay();
        used.computeIfPresent(address, (key, count) -> count > 1 ? count - 1 : null);
    }

    public synchronized int remaining(String address) {
        startNewDay();
        return Math.max(0, limit - used.getOrDefault(address, 0));
    }

    public int limit() {
        return limit;
    }

    private void startNewDay() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        if (!today.equals(day)) {
            used.clear();
            day = today;
        }
    }
}

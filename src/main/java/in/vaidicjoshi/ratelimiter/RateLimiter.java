package in.vaidicjoshi.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author Vaidic Joshi
 * @date 01/08/22
 */
public class RateLimiter {
    private final long allowedRate;
    private final Map<Integer, Map.Entry<Long, AtomicLong>> funcToTimestampAndTokenMap;
    private final String window;
    private final long windowInMilli;


    public RateLimiter(long allowedRate, String window) {
        this.window = window;
        this.allowedRate = allowedRate;
        this.funcToTimestampAndTokenMap = new ConcurrentHashMap<>();
        this.windowInMilli = getMilliFromWindow(window);
    }

    private long getMilliFromWindow(String window) {
        // split into val and chrono unit
        String[] split = window.split("((?<=\\d)(?=[A-Z]))");
        if (split.length != 2) {
            throw new InputMismatchException("Incorrect time window specified");
        }
        long val = Long.parseLong(split[0]);
        String chronoUnit = split[1];
        return switch (chronoUnit) {
            case "Sec" -> Duration.of(val, ChronoUnit.SECONDS).toMillis();
            case "Min" -> Duration.of(val, ChronoUnit.MINUTES).toMillis();
            case "Hrs" -> Duration.of(val, ChronoUnit.HOURS).toMillis();
            default -> throw new InputMismatchException("Incorrect time window specified");
        };
    }

    synchronized public <T extends Object, R extends Object> R enforceRateLimit(final T input, final int callingFunc) {
        Map.Entry<Long, AtomicLong> tsAndToken = funcToTimestampAndTokenMap.get(callingFunc);
        Long lastUpdateTime = tsAndToken.getKey();
        AtomicLong tokens = tsAndToken.getValue();

        long current = Instant.now().toEpochMilli();
        long timePassed = current - lastUpdateTime;
        if (timePassed > windowInMilli) {
            // rate limit is fully reset after each window
            tokens = new AtomicLong(allowedRate);
            lastUpdateTime = current;
        }
        tokens.decrementAndGet();
        Long finalLastUpdateTime = lastUpdateTime;
        AtomicLong finalTokens = tokens;
        funcToTimestampAndTokenMap.compute(callingFunc, (k, v) -> new AbstractMap.SimpleEntry<>(finalLastUpdateTime, finalTokens));
        System.out.println(tokens.get());
        if (tokens.get() < 0) {
            throw new RuntimeException("Max Allowed Rate Reached");
        } else {
            return (R) input;
        }
    }

    public <T extends Object, R extends Object> Function<T, R> wrap(Function<T, R> function) {
        int funcHashcode = function.hashCode();
        final Long now = Instant.now().toEpochMilli();
        // Initialize token bucket for function
        funcToTimestampAndTokenMap.computeIfAbsent(funcHashcode, (k) -> new AbstractMap.SimpleEntry<>(now, new AtomicLong(allowedRate)));
        return function.compose(val -> this.enforceRateLimit(val, funcHashcode));
    }
}


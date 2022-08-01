package in.vaidicjoshi.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.InputMismatchException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Vaidic Joshi
 * @date 01/08/22
 */
class RateLimiterTest {
    private RateLimiter rateLimiter;

    @BeforeEach
    void init() {
        rateLimiter = new RateLimiter(100, "1Min");
    }

    @Test
    public void testShouldFailWhenInvalidWindowIsSupplied() {
        assertThrows(InputMismatchException.class,
                () -> new RateLimiter(100, "1Minutes"),
                "Expected to throw InputMismatchException, but it didn't");
    }

    @Test
    public void testShouldNotFailWhenValidWindowIsSupplied() {
        assertDoesNotThrow(() -> new RateLimiter(100, "1Min"));
    }

    @Test
    public void testShouldReturnValidResult() {
        Function<Double, Double> sqrt = Math::sqrt;
        Function<Double, Double> rateLimitedSqrt = rateLimiter.wrap(sqrt);
        Double result = rateLimitedSqrt.apply(16.0);
        assertEquals(4.0, result, "Return value is not as expected");
    }

    @Test
    public void testShouldReturnValidResultWhenMultipleFunctionsAreRateLimited() {
        Function<Double, Double> sqrt = Math::sqrt;
        Function<Double, Double> rateLimitedSqrt = rateLimiter.wrap(sqrt);
        Function<Integer, Double> cube = k -> (double) k * k * k;
        Function<Integer, Double> rateLimitedCube = rateLimiter.wrap(cube);
        Function<Double, String> toString = k -> Double.toString(k);
        Function<Double, String> rateLimitedToString = rateLimiter.wrap(toString);

        assertEquals(4.0, rateLimitedSqrt.apply(16.0), "Return value is not as expected");
        assertEquals(64.0, rateLimitedCube.apply(4), "Return value is not as expected");
        assertEquals("204.0", rateLimitedToString.apply(204.0), "Return value is not as expected");
    }

    @Test
    public void testShouldAllowCallsWithinLimit() {
        Function<Integer, Integer> square = k -> k * k;
        final Function<Integer, Integer> rateLimitedSquare = rateLimiter.wrap(square);
        assertDoesNotThrow(() -> IntStream.range(0, 99).sequential().forEach(rateLimitedSquare::apply));
        rateLimiter = new RateLimiter(100, "1Min");
        final Function<Integer, Integer> rateLimitedSquareParallel = rateLimiter.wrap(square);
        assertDoesNotThrow(() -> IntStream.range(0, 99).parallel().forEach(rateLimitedSquareParallel::apply));
        rateLimiter = new RateLimiter(1, "1Min");
        final Function<Integer, Integer> anotherRateLimitedSquare = rateLimiter.wrap(square);
        assertDoesNotThrow(() -> IntStream.range(0, 1).sequential().forEach(rateLimitedSquare::apply));
    }

    @Test
    public void testShouldFailExcessRequest() {
        Function<Integer, Integer> square = k -> k * k;
        final Function<Integer, Integer> rateLimitedSquare = rateLimiter.wrap(square);
        assertThrows(RuntimeException.class, () -> IntStream.range(0, 101).sequential().forEach(rateLimitedSquare::apply));
        final Function<Integer, Integer> rateLimitedSquareParallel = rateLimiter.wrap(square);
        assertThrows(RuntimeException.class, () -> IntStream.range(0, 101).parallel().forEach(rateLimitedSquareParallel::apply));
        rateLimiter = new RateLimiter(1, "1Min");
        final Function<Integer, Integer> anotherRateLimitedSquare = rateLimiter.wrap(square);
        assertThrows(RuntimeException.class, () -> IntStream.range(0, 2).sequential().forEach(anotherRateLimitedSquare::apply));
        rateLimiter = new RateLimiter(1, "1Min");
        final Function<Integer, Integer> anotherRateLimitedSquareParallel = rateLimiter.wrap(square);
        assertThrows(RuntimeException.class, () -> IntStream.range(0, 2).parallel().forEach(anotherRateLimitedSquareParallel::apply));
    }

    @Test
    public void testShouldAllowRateLimitOnMultipleFunctions() {
        Function<Integer, Integer> square = k -> k * k;
        final Function<Integer, Integer> rateLimitedSquare = rateLimiter.wrap(square);
        Function<Integer, Integer> cube = k -> k * k * k;
        final Function<Integer, Integer> rateLimitedCube = rateLimiter.wrap(cube);
        assertDoesNotThrow(() -> IntStream.range(0, 99).sequential().forEach(rateLimitedSquare::apply));
        assertDoesNotThrow(() -> IntStream.range(0, 99).sequential().forEach(rateLimitedCube::apply));
    }


    @Test
    public void testShouldAllowAfterTimeWindow() throws InterruptedException {
        rateLimiter = new RateLimiter(10, "20Sec");
        Function<Integer, Integer> square = k -> k * k;
        final Function<Integer, Integer> rateLimitedSquare = rateLimiter.wrap(square);
        assertThrows(RuntimeException.class, () -> IntStream.range(0, 11).sequential().forEach(rateLimitedSquare::apply));
        Thread.sleep(20000);
        assertDoesNotThrow(() -> IntStream.range(0, 9).sequential().forEach(rateLimitedSquare::apply));
    }

    @Test
    public void testShouldRateLimitDuringConcurrentCall() throws InterruptedException {
        Function<Integer, Integer> unitFunction = k -> 1;
        final Function<Integer, Integer> rateLimitedUnitFunction = rateLimiter.wrap(unitFunction);

        ExecutorService service = Executors.newFixedThreadPool(25);
        int numberOfThreads = 250;
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    allowed.addAndGet(rateLimitedUnitFunction.apply(1));
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await();
        assertEquals(100, allowed.get());
        assertEquals(150, failed.get());
    }
}
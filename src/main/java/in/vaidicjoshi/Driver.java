package in.vaidicjoshi;

import in.vaidicjoshi.ratelimiter.RateLimiter;

import java.util.function.Function;

/**
 * @author Vaidic Joshi
 * @date 01/08/22
 */
public class Driver {

    public static void main(String[] args) {
        RateLimiter rateLimiter = new RateLimiter(101, "10Min");
        Function<Integer, Integer> square = k -> k * k;
        Function<Integer, Integer> cube = k -> k * k;
        Function<Integer, Integer> rateLimitedSquare = rateLimiter.wrap(square);
        Function<Integer, Integer> rateLimitedCube = rateLimiter.wrap(cube);
        for (int i = 0; i < 100; i++) {
            System.out.println(rateLimitedSquare.apply(4));
            System.out.println(rateLimitedCube.apply(4));
        }
    }
}

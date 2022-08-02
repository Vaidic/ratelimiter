# Rate Limiter
## Problem Statement
A rate limiter is used to restrict the rate at which a resource is accessed. 
The requirement is to implement a Rate Limiter that can wrap any function call 
and prevent calls in excess of the specified limit. First a rate limiter is 
configured with the allowed rate limit. as shown - 

`RateLimiter limiter = new RateLimiter(allowedRate, window)`

**For example:**

- `new RateLimiter(10, 10Sec)` - Allow 10 calls within a 10 sec window
- `new RateLimiter(50, 1Min)` - Allow 50 calls within a 1 min window

Once configured it can be used to wrap any Function `F<Input, Output>` to apply a rate limit.

**Example**:

`Function<I,O> rateLimitedFunc = rateLimiter.wrap(Function<I,O> func);`

**For example, say we have a function**

`Function<Integer, Integer> square = (x)->x*x;`

We can construct a **ratelimited** version of this via

`Function<Integer,Integer> rateLimitedSquare = rateLimiter.wrap(square);`

The wrap method takes a Function and returns a Function with **same signature**.

The clients can then call the wrapped function in the same way as the original function.

**Example**: 

`f.apply(4)`

When this wrapped function receives more calls than allowed it should fail the excess requests.
After implementing the function write test cases to verify the functionality of RateLimiter.

## Solution Assumptions
1. Rate Limit is applied on number of function calls
2. The rate limit is not imposed on users or ip 
3. The function that can be rate limited is a function that accepts a single argument - `Function<I,O>`
4. The Rate Limit is initialized using constructor of class `RateLimiter` and this is not exposed as API yet 
5. The client has a way to call the wrapped function


## Solution Description & Test Files
The core logic to `wrap` any given function and `enforceRateLimit` on it is in the class `RateLimiter`.

The `wrap` function creates a `composition` of functions such that the rate limit is checked 
before each and every call to the function. The calls to the functions are identified using 
`hashcode` of function(this can be changed to any other way for uniquely identifying the function call).

The logic to enforce rate limit used `fixed-window` algorithm, where the rate is reset after 
each window, computed from start of enabling rate limit, gets elapsed.
The memory footprint required for this approach is of order `O(n)` where `n` is  the 
`number of functions wrapped to enable rate limit`.<br/>
We store two fields - `last timestamp`and `allowed rate/ tokens available` per wrapped function.

In addition, a `Driver` class file is provided to run and test the rate limiter logic on the fly.

The test are in `RateLimiterTest` class.


## Further Improvements 
1. The current implementation uses a `synchronized` method to enforce thread safety, this can be further improved by having synchronized blocks inside method instead. 
2. The current implementation uses `fixed window` algorithm, which is simple
and has advantage over a `leaky-bucket or token-bucket` algorithm but it allows `2 times burst traffic` at window boundaries.
This can be improved by using a `sliding-window` algorithm that uses approximation from past window to enforce rate limit. 
_**ref**_ - [Cloudflare Blog](https://blog.cloudflare.com/counting-things-a-lot-of-different-things/)
3. Additional logging can be done for easy debugging.

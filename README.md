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

## Solution Description & Test Files

## Further Improvements 

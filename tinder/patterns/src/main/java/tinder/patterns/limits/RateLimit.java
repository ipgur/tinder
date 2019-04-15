/*
 * Copyright 2019 Raffaele Ragni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tinder.patterns.limits;

import java.util.Optional;
import static java.util.Optional.empty;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Limits the rate of which a function is being called.
 *
 * This limitation can be intended in 2 ways:
 *  - blocking: make the functions wait as long as the limit is reached, queuing them up.
 *  - failing: execute a given function (can be a return of an exception, or a back pressure command to some library)
 *             as the limit is reached.
 *
 * The limitation itself can be expressed as a number of calls in a time period.
 *
 * A function is considered executed only if does not throw an exception.
 * Which means errors won't count against the increment of the rate limit.
 *
 * @author Raffaele Ragni
 * @param <T> type return of the supplier function to decorate
 */
public class RateLimit<T> implements Supplier<T> {

  private static final Logger LOGGER = Logger.getLogger(RateLimit.class.getName());

  private enum Type {BLOCKING, FAILING}

  private final Supplier<T> supplier;
  private final Type type;
  private final long maxCalls;
  private final long timeWindowMS;
  private final Optional<Supplier<T>> failureFn;

  // We keep a count being incremented and we decrement it periodically on the timeWindowMS
  // with a side thread and a low boundary of 0.
  // Each RateLimit instance has its own thread
  // We could even reset it to 0 in each timeWindowMS, but by doing a subtraction with a max(x, 0) allows
  // a more granular sliding of a time window.
  // Also the differente in those operations is minimal, subtraction against assignment as a constant periodic thread
  // is needed anyway.
  private final AtomicLong currentTimeWindowCount = new AtomicLong(0);

  private RateLimit(Type type, long maxCalls, long timeWindowMS, Supplier<T> supplier, Optional<Supplier<T>> failureFn) {

    if (supplier == null) {
      throw new IllegalArgumentException("supplier function is required.");
    }
    if (maxCalls <= 0) {
      throw new IllegalArgumentException("maxCalls must be greater than 0.");
    }
    if (timeWindowMS <= 0) {
      throw new IllegalArgumentException("timeWindowMS must be greater than 0.");
    }

    this.type = type;
    this.supplier = supplier;
    this.maxCalls = maxCalls;
    this.timeWindowMS = timeWindowMS;
    this.failureFn = failureFn;

    // We cannot know when the rateLimit is destroyed, so we just apply a system shutdown hook.
    // The thread is a very light decrementing one anyway.
    // Maybe in the future will export this control of the the user, but for now want to keep its usage simple and
    // transparent.
    Runnable periodPassed = () -> {
      try {
        // Since this thread is not synchronized it could at worst let some extra calls through,
        // but not a big deal because it's probably within a tolerance limit anyway.
        // And this way the performance is kept good as it's async.
        long count = currentTimeWindowCount.get();
        count -= maxCalls;
        count = count < 0 ? 0 : count;
        currentTimeWindowCount.set(count);
      } catch(RuntimeException e) { }
    };

    // Schedule periodically, each timeWindowMS
    ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
    scheduled.scheduleAtFixedRate(periodPassed, timeWindowMS, timeWindowMS, TimeUnit.MILLISECONDS);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { scheduled.shutdownNow(); }));
  }

  /**
   * Creates a rate limit that blocks as long as the limit is pushed above the max for a time window.
   * @param <T> return type of your supplier
   * @param maxCalls maximum calls limit for the time window
   * @param timeWindowMS time window period in milliseconds
   * @param supplier your supplier
   * @return Decorated supplier.
   */
  public static <T> Supplier<T> asBlocking(long maxCalls, long timeWindowMS, Supplier<T> supplier) {
    return new RateLimit(Type.BLOCKING, maxCalls, timeWindowMS, supplier, empty());
  }

  /**
   * Creates a rate limit that fails as long as the limit is pushed above the max for a time window.
   * @param <T> return type of your supplier
   * @param maxCalls maximum calls limit for the time window
   * @param timeWindowMS time window period in milliseconds
   * @param failureFn the failure case supplier. Either trow an exception or return something (same as your supplier)
   * @param supplier your supplier
   * @return Decorated supplier.
   */
  public static <T> Supplier<T> asFailing(long maxCalls, long timeWindowMS, Supplier<T> failureFn, Supplier<T> supplier) {
    return new RateLimit(Type.FAILING, maxCalls, timeWindowMS, supplier, Optional.of(failureFn));
  }

  /**
   *
   */
  @Override
  public T get() {

    if (type == Type.FAILING && currentTimeWindowCount.get() >= maxCalls) {
      return failureFn.orElseThrow(() -> new IllegalStateException("no failureFn was specified")).get();
    }
    else if (type == Type.BLOCKING) {
      // It's a while loop because in the case of BLOCKING we are waiting until the situation unlocks.
      while (currentTimeWindowCount.get() >= maxCalls) {
        try {
          // Arbitrary 1/10th of the time window wait time.
          synchronized (this) { wait(timeWindowMS/10); }
        } catch (InterruptedException ex) { }
      }
    }

    try {
      return supplier.get();
    } finally {
      // Since we are out of the while, then we can go with execution,
      // increment and execute.
      // We add the increment here because only the real execution counts for +1
      // If the function threw an exception for some reason, we don't count that as an execution.
      // Maybe later add a flag for that?
      currentTimeWindowCount.incrementAndGet();
    }
  }

}

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
package tinder.patterns.resilience;

import static java.lang.String.format;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Attempt the same operation in a sequence as long as it is throwing an exception.
 * No thread created nor started in this class.
 * 
 * Note: only RuntimeException is allowed from the operation, no checked exceptions.
 * 
 * The default number of retries is 5.
 * You can change it by calling times(number).
 * A new retry object will be returned with such configuration.
 * 
 * The default wait time between retries is 0.
 * It can be configured, it is in milliseconds.
 * 
 * The default exception behavior is to throw the last caught one.
 * 
 * Given an operation and a number of attempts, this class will attempt the operation again until it is successful.
 * The operation is successful when it throws no exception.
 * 
 * In case of exception, the Retry will continue, until the number of attempt is reached, to trigger the operation again.
 * When the exception occurs from the operation, it will be stored so that based on the initial configuration of Retry
 * either the first or the last exception will be thrown as a final result.
 * If the number of attempts are reached, then either of those exceptions are going to be thrown from this class.
 * 
 * A circuit breaker can also be specified.
 * It is a function that given the exception being thrown by the operation, returns true if such exception must break
 * the circuit, and thus forcing the Retryable to always stop and throw it.
 * 
 * @param <T> the type returned from the operation, if any
 * 
 * @author Raffaele Ragni
 */
public class Retryable<T> implements Supplier<T> {

  private static final Logger LOGGER = Logger.getLogger(Retryable.class.getName());
  
  private static final long DEFAULT_RETRIES = 5L;
  private static final long DEFAULT_DELAY = 0L;
  
  // Some identifying name for this operation. Optional.
  private final String name;
  // The operation to run
  private final Supplier<T> operation;
  // The number of attempts to reach before finally failing
  private final long maxAttempts;
  // Delay to wait between retries
  private final long delay;
  // when throwFirst is true, the first received exception is the one being saved
  // and the one being thrown at the end if maxAttempts is reached.
  // Otherwhise, when false, the last caught exception from the operation is the one that will be thrown.
  private final boolean throwFirst;
  // A curcuit breaker will stop the retry attempts when the exception passed matches some kind of logic
  // (the caller defines it through this function).
  // A result of true means the Retryable must STOP and throw such exception immediately.
  private final Function<Throwable, Boolean> circuitBreaker;
    
  private RuntimeException caught = new IllegalStateException(
      "Operation has failed without throwing any exception or returning any data." +
      " There aren't any known explanations of how that could happen.");
  private boolean firstException = true;
  
  private Retryable(String name, Supplier<T> operation,
      long maxAttempts, long delay, boolean throwFirst,
      Function<Throwable, Boolean> circuitBreaker) {
    this.name = name;
    this.operation = operation;
    this.maxAttempts = maxAttempts;
    this.delay = delay;
    this.throwFirst = throwFirst;
    this.circuitBreaker = circuitBreaker;
  }
  
  /**
   * Returns a Retryable for the specified operation
   * @param operation the operation to retry
   * @param <T> the type returned from the operation, if any
   * @return the Retryable
   */
  public static <T> Retryable<T> of(Supplier<T> operation) {
    if (operation == null) {
      throw new IllegalArgumentException("You must provide an operation for this class, operation was null.");
    }
    return new Retryable("retryable", operation, DEFAULT_RETRIES, DEFAULT_DELAY, false, null);
  }
  
  /**
   * Give a name to this operation.
   * This comes useful when logs will print info or error/warnings about this operation/
   * @param name the name to give to the operation
   * @return A new Retryable with this new configuration
   */
  public Retryable<T> named(String name) {
    if (name == null) {
      throw new IllegalArgumentException("You must provide a name, name was null.");
    }
    return new Retryable(name, operation, maxAttempts, delay, throwFirst, circuitBreaker);
  }
  
  /**
   * Sets the amount of times to retry the operation.
   * Default is 5.
   * @param maxAttempts the amount of times to retry
   * @return A new Retryable with this new configuration
   */
  public Retryable<T> times(long maxAttempts) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("Can't try an amount less than 1 times. Use a number greater than 0.");
    }
    return new Retryable(name, operation, maxAttempts, delay, throwFirst, circuitBreaker);
  }
  
  /**
   * Adds a delay to the retries of the operations.
   * The delay is the time that is waited between multiple retry attempts.
   * But The first try still goes immediately
   * @param delay wait time in milliseconds
   * @return A new Retryable with this new configuration
   */
  public Retryable<T> delay(long delay) {
    if (delay < 0) {
      throw new IllegalArgumentException("Can't wait for a negative time, provide a positive number or 0.");
    }
    return new Retryable(name, operation, maxAttempts, delay, throwFirst, circuitBreaker);
  }
  
  /**
   * Sets the first exception to be the one to be thrown in case of total failure.
   * That is, the first time operation fails and throws an exception, such exception gets saved.
   * If the number of attempts reach the maximum, that exception will be the one thrown from this class.
   * Default is throw last.
   * @return A new Retryable with this new configuration
   */
  public Retryable<T> throwFirst() {
    return new Retryable(name, operation, maxAttempts, delay, true, circuitBreaker);
  }
  
  /**
   * Sets the last exception to be the one to be thrown in case of total failure.
   * That is, the last time the operation fails and throws an exception, that will be the exception being
   * thrown from this class.
   * Default is throw last.
   * @return A new Retryable with this new configuration
   */
  public Retryable<T> throwLast() {
    return new Retryable(name, operation, maxAttempts, delay, false, circuitBreaker);
  }
  
  /**
   * Applies a circuit breaker.
   * This is a function that can stop the retry in case of the exception matching some logic.
   * The caller defines such function and logic.
   * @param circuitBreaker the function that can break the circuit
   * @return A new Retryable with this new configuration
   */
  public Retryable<T> circuitBreaker(Function<Throwable, Boolean> circuitBreaker) {
    return new Retryable(name, operation, maxAttempts, delay, throwFirst, circuitBreaker);
  }
  

  /**
   * An alias to the retry() for a Supplier so it can be used in functional cases.
   * @return the provided result from the operation
   */
  @Override
  public T get() {
    return retry();
  }
  
  /**
   * Retries the operation until the number of attempts is reached.
   * If that happens, then the exception (first or last, based on configuration) is thrown from here.
   * @return the provided result from the operation
   */
  public T retry() {
    for (int i = 0; i < maxAttempts; i++) {
      // Attempt the operation and catch the exceptions
      try {
        return operation.get();
      } catch (RuntimeException ex) {
        LOGGER.warning(format(
            "The operation \"%s\" was attempted %d time/s, but fails with: \"%s\"",
            name,
            i + 1,
            ex.getMessage()));
        manageException(ex);
      }
      waitForRetry(caught);
    }
    // All failed, throw whatever we had.
    throw caught;
  }
  
  private void manageException(RuntimeException ex) {
    if (circuitBreaker != null && circuitBreaker.apply(ex)) {
      // breaking the circuit will immediately throw the exception
      throw ex;
    }
    if (throwFirst) {
      if (firstException) {
        caught = ex;
      }
    } else {
      caught = ex;
    }
    firstException = false;
  }
  
  private void waitForRetry(RuntimeException caught) {
    // Wait for a delay until the next try, if specified.
    if (delay > 0L) {
      try {
        LOGGER.info(() -> format(
          "Waiting %dms to retry again operation \"%s\"...",
          delay,
          name));
        TimeUnit.MILLISECONDS.sleep(delay);
      } catch (InterruptedException ex) {
        // Propagate the thread interruption
        Thread.currentThread().interrupt();
        // In any case throw what we have
        throw caught;
      }
    }
  }

  
}

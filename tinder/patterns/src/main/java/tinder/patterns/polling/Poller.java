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
package tinder.patterns.polling;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Dynamic poller that expands and contracts its waiting time based on the min and max parameters.
 * You can use 0 for min and/or max.
 *
 * @param <T> the type passed between the producer and consumer.
 *
 * @author Raffaele Ragni
 */
public class Poller<T> implements Runnable {

  private final long min;
  private final long max;
  private final Supplier<Optional<T>> producer;
  private final Consumer<T> consumer;

  private long currentWait;

  public Poller(
      long min, long max,
      Supplier<Optional<T>> producer, Consumer<T> consumer) {
    this.min = min;
    this.max = max;
    this.producer = producer;
    this.consumer = consumer;
    pollIntervalReset();
  }

  /**
   * Sets the minimum time.
   * @param min number of seconds to wait minimally
   * @return new poller
   */
  public Poller min(long min) {
    return min(min, TimeUnit.SECONDS);
  }

  /**
   * Sets the maximum time.
   * @param max number of seconds to wait maximally
   * @return new poller
   */
  public Poller max(long max) {
    return max(max, TimeUnit.SECONDS);
  }

  /**
   * Sets the minimum time.
   * @param min number of time units to wait minimally
   * @param timeUnit which time unit to use
   * @return new poller
   */
  public Poller min(long min, TimeUnit timeUnit) {
    min = TimeUnit.MILLISECONDS.convert(min, timeUnit);
    if (min < 0) {
      throw new IllegalArgumentException("min can't be negative");
    }
    return new Poller(min, max, producer, consumer);
  }

  /**
   * Sets the maximum time.
   * @param max number of time units to wait maximally
   * @param timeUnit new poller which time unit to use
   * @return new poller
   */
  public Poller max(long max, TimeUnit timeUnit) {
    max = TimeUnit.MILLISECONDS.convert(max, timeUnit);
    if (max < 0) {
      throw new IllegalArgumentException("max can't be negative");
    }
    if (max < min) {
      throw new IllegalArgumentException("max cannot be less than min");
    }
    return new Poller(min, max, producer, consumer);
  }

  /**
   * Creates a new default Poller.
   * You will still need to specify a min+max to have wait times.
   * The default is to wait nothing (min and max being 0)
   * @param <T> Type of data passed between producer and consumer.
   * @param producer the producer of some data (as optional). If data is present then the poller keeps going,
   *                 if it's empty() it goes into larder wait times.
   * @param consumer consumer of the returned type, without the optional.
   * @return new poller.
   */
  public static <T> Poller<T> poller(Supplier<Optional<T>> producer, Consumer<T> consumer) {
    return new Poller(0L, 0L, producer, consumer);
  }

  /**
   * Runs the poller.
   * It will keep running forever until the thread gets interrupted so it's recommended to put this inside a thread.
   */
  @Override
  public void run() {

    // Reset to min
    pollIntervalReset();

    while (!Thread.currentThread().isInterrupted()) {

      Optional<T> polledValue = producer.get();
      polledValue.ifPresent(consumer);

      // If a value was found, we reset the wait to the min because it means there could still be more data to process.
      // if no value was found though, we step tp max because no data is there and wait longer.
      if (polledValue.isPresent()) {
        // Reset to min
        pollIntervalReset();
      } else {
        // Go to max
        pollIntervalStepUp();
      }
      // And now just wait...
      pollIntervalWait();
    }

  }

  private void pollIntervalWait() {
    if (currentWait > 0L) {
      try {
        TimeUnit.MILLISECONDS.sleep(currentWait);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void pollIntervalReset() {
    currentWait = min;
  }

  private void pollIntervalStepUp() {
    currentWait = max;
  }

}

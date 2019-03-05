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
package tinder.core.modules.metrics;

import com.timgroup.statsd.StatsDClient;
import java.util.function.Supplier;

/**
 * Helper wrappers for quick  use of statsd in the api projects.
 * @author Raffaele Ragni
 */
public class StatsDHelper {

  final StatsDClient client;

  public StatsDHelper(StatsDClient client) {
    this.client = client;
  }

  /**
   * Wraps around a function to count a timed aspect metric, using statsd.
   * It always measures, even if the function throws exception.
   * The metric is always in milliseconds.
   * @param <T> return type of the function
   * @param metric metric name
   * @param fn the function to wrap around
   * @return the return of the function wrapped
   */
  public <T> T timedAround(String metric, Supplier<T> fn) {
    long t = System.currentTimeMillis();
    try {
      return fn.get();
    } finally {
      t = System.currentTimeMillis() - t;
      client.gauge(metric, t);
    }
  }

  /**
   * Counting up increment of a metric at each call of this function.
   * It always counts, even if the function throws exception.
   * @param <T> return type of the function
   * @param metric metric name
   * @param fn the function to wrap around
   * @return the return of the function wrapped
   */
  public <T> T counterAround(String metric, Supplier<T> fn) {
    try {
      return fn.get();
    } finally {
      client.increment(metric);
    }
  }

}

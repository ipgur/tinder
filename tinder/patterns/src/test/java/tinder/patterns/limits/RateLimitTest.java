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

import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class RateLimitTest {

  @Test
  public void testConstructors() {

    RateLimit.asFailing(1, 1, () -> 1, () -> 1);

    RateLimit.asBlocking(1, 1, () -> 1);

    IllegalArgumentException ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      RateLimit.asFailing(0, 1, () -> 1, () -> 1);
    });
    Assertions.assertEquals("maxCalls must be greater than 0.", ex1.getMessage());

    IllegalArgumentException ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      RateLimit.asFailing(1, 0, () -> 1, () -> 1);
    });
    Assertions.assertEquals("timeWindowMS must be greater than 0.", ex2.getMessage());

    IllegalArgumentException ex3 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      RateLimit.asFailing(1, 1, () -> 1, null);
    });
    Assertions.assertEquals("supplier function is required.", ex3.getMessage());

  }

  @Test
  public void testFailingMethod() {

    Supplier<Integer> fn = RateLimit.asFailing(1, 50, () -> 2, () -> 1);

    // count becomes 1
    int i1 = fn.get();
    Assertions.assertEquals(1, i1);
    // count is >= max before starting
    // So this one triggers the fail function which returns 2 instead of 1.
    int i2 = fn.get();
    Assertions.assertEquals(2, i2);

  }

  @Test
  public void testBlockingMethod() {
    // Generally these are BAD tests, because relies on executor performance,
    // but we're talking 500ms for a very simple two calls of get() of a static number,
    // should be well within the time of wait.
    long start = System.currentTimeMillis();
    Supplier<Integer> fn = RateLimit.asBlocking(1, 500, () -> 1);
    fn.get();
    // This second one should trigger a general wait of 500ms at least
    fn.get();
    long end = System.currentTimeMillis();
    long took = end - start;
    Assertions.assertTrue(took >= 500, "Time taken >= 500ms? Took: " + took + "ms");
  }

}

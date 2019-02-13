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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class RetryableTest {
  
  @Test
  public void testValidations() {
    
    IllegalArgumentException ex;
    
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Retryable.of(null);
    });
    Assertions.assertEquals("You must provide an operation for this class, operation was null.", ex.getMessage());
    
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Retryable.of(() -> true).named(null);
    });
    Assertions.assertEquals("You must provide a name, name was null.", ex.getMessage());
    
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Retryable.of(() -> true).times(0);
    });
    Assertions.assertEquals("Can't try an amount less than 1 times. Use a number greater than 0.", ex.getMessage());
    
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Retryable.of(() -> true).times(-1);
    });
    Assertions.assertEquals("Can't try an amount less than 1 times. Use a number greater than 0.", ex.getMessage());
    
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Retryable.of(() -> true).delay(-1);
    });
    Assertions.assertEquals("Can't wait for a negative time, provide a positive number or 0.", ex.getMessage());
    
  }
  
  @Test
  public void testFailure() {
    // No matter ho many times we try, this must always throw an excepton.
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      Retryable.of(() -> {throw new RuntimeException("surprise!");})
          .named("no surprises")
          .times(3)
          .retry();
    });
    Assertions.assertEquals("surprise!", ex.getMessage());
  }

  @Test
  public void testFirstException() {
    // No matter ho many times we try, this must always throw an excepton.
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      // This is a trick to have a mutable final counter between lambdas
      final long[] counter = new long[] {0L};
      // This must not throw an exception
      Retryable.of(() -> {
        counter[0]++;
        if (counter[0] > 1) {
          throw new RuntimeException("secondary exceptions!");
        } else {
          throw new RuntimeException("first exception!");
        }
      }).named("fails at 1st").times(3).throwFirst().retry();
    });
    Assertions.assertEquals("first exception!", ex.getMessage());
  }

  @Test
  public void testLastException() {
    // No matter ho many times we try, this must always throw an excepton.
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      // This is a trick to have a mutable final counter between lambdas
      final long[] counter = new long[] {0L};
      // This must not throw an exception
      Retryable.of(() -> {
        counter[0]++;
        if (counter[0] < 2) {
          throw new RuntimeException("secondary exceptions!");
        } else {
          throw new RuntimeException("last exception!");
        }
      }).named("fails at last").times(3).throwLast().retry();
    });
    Assertions.assertEquals("last exception!", ex.getMessage());
  }
  
  @Test
  public void testOKAtFirstTry() {
    // This must not throw an exception
    boolean value = Retryable.of(() -> true)
        .named("all OK")
        .times(3)
        .retry();
    Assertions.assertTrue(value, "value was wrong");
  }
  
  @Test
  public void testOKZeroDelay() {
    // This must not throw an exception
    boolean value = Retryable.of(() -> true)
        .named("all OK")
        .times(3)
        .delay(0)
        .retry();
    Assertions.assertTrue(value, "value was wrong");
  }
  
  @Test
  public void testOKAtFirstTryWithGet() {
    // This must not throw an exception
    boolean value = Retryable.of(() -> true)
        .named("all OK")
        .times(3)
        .get();
    Assertions.assertTrue(value, "value was wrong");
  }
  
  @Test
  public void testOKAtLastTry() {
    // This is a trick to have a mutable final counter between lambdas
    final long[] counter = new long[] {0L};
    // This must not throw an exception
    Retryable.of(() -> {
      counter[0]++;
      if (counter[0] < 3) {
        throw new RuntimeException("works at 3rd!");
      }
      return null;
    }).named("works at 3rd").times(3).retry();
  }
  
  @Test
  public void testRealDelay() {
    // No matter ho many times we try, this must always throw an excepton.
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      Retryable.of(() -> {throw new RuntimeException("surprise!");})
          .named("no surprises")
          .times(3)
          .delay(1)
          .retry();
    });
    Assertions.assertEquals("surprise!", ex.getMessage());
  }
  
  @Test
  public void testInterruption() {
    // No matter ho many times we try, this must always throw an excepton.
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      Thread runningRetry = new Thread(() -> {
        Retryable.of(() -> {throw new RuntimeException("surprise!");})
            .named("no surprises")
            .times(3)
            .delay(10)
            .retry();
      });
      TimeUnit.MILLISECONDS.sleep(30);
      // Trick to pass stuff between lambdas
      RuntimeException[] exys = new RuntimeException[] {null};
      runningRetry.setUncaughtExceptionHandler((t, exy) -> {exys[0] = (RuntimeException) exy;});
      runningRetry.start();
      runningRetry.interrupt();
      runningRetry.join();
      if (exys != null) {
        throw exys[0];
      }
    });
    Assertions.assertEquals("surprise!", ex.getMessage());
  }
  
  @Test
  public void testCircuitBreaker() {
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      // This is a trick to have a mutable final counter between lambdas
      final long[] counter = new long[] {0L};
      // This must not throw an exception
      Retryable.of(() -> {
        counter[0]++;
        if (counter[0] < 2) {
          throw new RuntimeException("primary exception!");
        } else {
          throw new RuntimeException("last exception!");
        }
      }).named("fails with breaker").times(3).circuitBreaker(e -> true).retry();
    });
    Assertions.assertEquals("primary exception!", ex.getMessage());
  }
  
  @Test
  public void testCircuitBreakerNeverBreaking() {
    RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
      // This is a trick to have a mutable final counter between lambdas
      final long[] counter = new long[] {0L};
      // This must not throw an exception
      Retryable.of(() -> {
        counter[0]++;
        if (counter[0] < 2) {
          throw new RuntimeException("primary exception!");
        } else {
          throw new RuntimeException("last exception!");
        }
      }).named("fails with breaker").times(3).circuitBreaker(e -> false).retry();
    });
    Assertions.assertEquals("last exception!", ex.getMessage());
  }
  
}

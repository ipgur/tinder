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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class PollerTest {
  
  @Test
  public void testPoller() throws InterruptedException {
    
    Poller<Long> poller = Poller.poller(new LimitedSupply(5), i -> {});
    
    Thread thread = new Thread(poller);
    thread.start();
    TimeUnit.MILLISECONDS.sleep(10);
    thread.interrupt();
    thread.join();
    
    poller = Poller.poller(new LimitedSupply(5), i -> {})
        .min(10, TimeUnit.MILLISECONDS)
        .max(10, TimeUnit.MILLISECONDS);
    
    thread = new Thread(poller);
    thread.start();
    TimeUnit.MILLISECONDS.sleep(100);
    thread.interrupt();
    thread.join();
  }
  
  @Test
  public void testValidations() {
    
    Poller.poller(new LimitedSupply(5), i -> {})
        .min(10, TimeUnit.MILLISECONDS)
        .max(10, TimeUnit.MILLISECONDS);
    
    IllegalArgumentException ex;
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Poller.poller(new LimitedSupply(5), i -> {})
          .min(-1, TimeUnit.MILLISECONDS);
    });
    Assertions.assertEquals("min can't be negative", ex.getMessage());
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Poller.poller(new LimitedSupply(5), i -> {})
          .max(-1, TimeUnit.MILLISECONDS);
    });
    Assertions.assertEquals("max can't be negative", ex.getMessage());
    
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Poller.poller(new LimitedSupply(5), i -> {})
          .min(10, TimeUnit.MILLISECONDS)
          .max(9, TimeUnit.MILLISECONDS);
    });
    Assertions.assertEquals("max cannot be less than min", ex.getMessage());
  }

}

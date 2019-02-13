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

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;

/**
 * Utility class for testing.
 * Provides a limited supply of values.
 * @author Raffaele Ragni
 */
public class LimitedSupply implements Supplier<Optional<Long>> {
  
  private final List<Long> queue;

  public LimitedSupply(long amount) {
    Random random = new Random(System.currentTimeMillis());
    this.queue = random.longs()
        .mapToObj(Long::valueOf)
        .limit(amount)
        .collect(toList());
  }

  @Override
  public Optional<Long> get() {
    if (queue.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(queue.remove(0));
  }
  
  
}

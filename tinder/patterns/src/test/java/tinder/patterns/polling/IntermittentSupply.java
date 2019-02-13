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
import java.util.function.Supplier;

/**
 * Utility class for testing.
 * Provides values every skip steps
 * @author Raffaele Ragni
 */
public class IntermittentSupply implements Supplier<Optional<Long>> {
  
  private final long skip;
  private long current;
  
  public IntermittentSupply(long skip) {
    this.skip = skip;
    current = 0;
  }

  @Override
  public Optional<Long> get() {
    current++;
    if (current > skip) {
      current = 0;
      return Optional.of(1L);
    }
    return Optional.empty();
  }
  
  
}

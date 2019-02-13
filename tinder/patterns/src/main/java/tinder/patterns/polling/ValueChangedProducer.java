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

import static java.lang.String.format;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * This is an easy wrapper for mapping different returned values to the optional supplier required for a poller to
 * understand whether to keep pushing for shorter times or not.
 * Use this if your producer keeps returning the same data and you want to consider only the first change in value.
 * Example: ValueChangedProducer.map(-yourSupplier-)
 * @author Raffaele Ragni
 */
public class ValueChangedProducer<T> implements Supplier<Optional<T>> {

  private static final Logger LOGGER = Logger.getLogger(ValueChangedProducer.class.getName());
  
  private final Supplier<T> dataSupplier;
  private boolean firstValue = true;
  private T previousValue;
  
  private ValueChangedProducer(Supplier<T> dataSupplier) {
    this.dataSupplier = dataSupplier;
  }

  /**
   * Builds a producer that returns non-empty Optional only when the returned value from the supplier is changed.
   * Keep in mind that this function will keep an internal reference to such value so careful about pointers and
   * garbage collection.
   * @param <T> the type of the value
   * @param producer the producer that will return the values directly
   * @return the producer that will apply the trigger logic.
   */
  public static <T> ValueChangedProducer<T> map(Supplier<T> producer) {
    return new ValueChangedProducer(producer);
  }
  
  @Override
  public Optional<T> get() {
    
    T currentValue = dataSupplier.get();
    // Optionals can't contain nulls.
    if (currentValue == null) {
      return Optional.empty();
    }
    
    // We treat the first time differently because because the previousValue variable is also initialized as null.
    // By using a boolean we cover the initial case.
    if (firstValue) {
      firstValue = false;
      previousValue = currentValue;
      LOGGER.fine(() -> format("First value received: %s", currentValue));
      return Optional.of(currentValue);
    }
    
    // If value was changed return the value.
    if (!currentValue.equals(previousValue)) {
      previousValue = currentValue;
      LOGGER.fine(() -> format("Valued changed from %s to %s", previousValue, currentValue));
      return Optional.of(currentValue);
    }
    
    // No value was changed, return empty.
    // The Poller will not trigger as long as we keep returning empty.
    return Optional.empty();
  }
  
}

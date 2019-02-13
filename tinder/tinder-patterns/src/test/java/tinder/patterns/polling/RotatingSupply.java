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
import java.util.function.Supplier;

/**
 *
 * @author Raffaele Ragni
 */
public class RotatingSupply<T> implements Supplier<T> {
  
  private final List<T> items;
  private int current;

  public RotatingSupply(List<T> items) {
    if (items.isEmpty()) {
      throw new IllegalArgumentException("provide a list that is not empty");
    }
    this.items = items;
    current = 0;
  }

  @Override
  public T get() {
    if (current >= items.size()) {
      current = 0;
    }
    return items.get(current++);
  }
  
}

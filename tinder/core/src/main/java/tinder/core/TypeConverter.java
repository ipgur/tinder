/*
 * Copyright 2018 Raffaele Ragni.
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
package tinder.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simple type converter for basic types.
 * @author Raffaele Ragni
 */
public interface TypeConverter {

  /**
   * Converts from string to anything.
   * Override this method to make your own converter.
   * Use the annotation @Converter(YourClass.class) on top of the @REST class to specify which converter to use.
   * YourClass must implement this interface.
   * @param <T> The converted type returned
   * @param value the string input
   * @param clazz the class of the returned item
   * @return the converted value
   */
  @SuppressWarnings("unchecked")
  default <T> T fromString(String value, Class<T> clazz) {

    // Here a simple default implementation for the common cases

    // null is always null no matter the type
    if (value == null) {
      return null;
    }

    // Strings need no conversion
    if (clazz == String.class) {
      return (T) value;
    }

    // If the type has a valueOf with a string as a parameter, just go with that one
    // All the base java types have it.
    try {
      Method valueOf = clazz.getMethod("valueOf", String.class);
      return (T) valueOf.invoke(null, value);
    } catch (NoSuchMethodException
        | SecurityException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException ex) {
      // Warning or nothing?
      throw new RuntimeException(ex);
    }
  }

}

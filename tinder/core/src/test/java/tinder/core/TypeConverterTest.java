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
package tinder.core;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class TypeConverterTest {

  @Test
  public void testConverter() {

    String s;
    Long l;
    TypeConverter converter = new TypeConverter() {};

    s = converter.fromString(null, String.class);
    Assertions.assertEquals(null, s);

    s = converter.fromString("teststring", String.class);
    Assertions.assertEquals("teststring", s);

    l = converter.fromString("5", Long.class);
    Assertions.assertEquals(Long.valueOf(5L), l);

    RuntimeException ex;
    ex = Assertions.assertThrows(RuntimeException.class, () -> {
      converter.fromString("0.1", BigDecimal.class);
    });
    Assertions.assertTrue(ex.getMessage().contains("NoSuchMethodException"));
    Assertions.assertTrue(ex.getMessage().contains("valueOf(java.lang.String)"));

  }

}

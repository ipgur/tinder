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


import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class ValueChangedPollOperationTest {

  @Test
  public void testValueChanges() {
    Supplier<Long> rotatingSupply = new RotatingSupply<>(Arrays.asList(1L, 2L, 2L, 1L, 1L, 2L, null));
    ValueChangedProducer<Long> valueChangedOperation = ValueChangedProducer.map(rotatingSupply);
    Assertions.assertEquals(Optional.of(1L), valueChangedOperation.get());
    Assertions.assertEquals(Optional.of(2L), valueChangedOperation.get());
    Assertions.assertEquals(Optional.empty(), valueChangedOperation.get());
    Assertions.assertEquals(Optional.of(1L), valueChangedOperation.get());
    Assertions.assertEquals(Optional.empty(), valueChangedOperation.get());
    Assertions.assertEquals(Optional.of(2L), valueChangedOperation.get());
    Assertions.assertEquals(Optional.empty(), valueChangedOperation.get());
  }
  
}

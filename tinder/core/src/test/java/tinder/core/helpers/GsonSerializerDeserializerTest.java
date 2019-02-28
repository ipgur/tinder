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
package tinder.core.helpers;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the GsonSerializers that were not included in the ported classes
 * @author Raffaele Ragni
 */
public class GsonSerializerDeserializerTest {

  @Test
  public void testExtraCases() {

    GsonSerializer<TestData> serializer = new GsonSerializer<>();
    GsonDeserializer<TestData> deserializer = new GsonDeserializer<>();

    serializer.serialize(new TestData());
    new GsonSerializer<Object>().serialize((Object) (new byte[]{1, 2, 3}));
    new GsonSerializer<Object>().serialize((Object) (new char[]{'1', '2', '3'}));
    serializer.writeValueAsBytes(new TestData());

    Assertions.assertThrows(JsonSyntaxException.class, () -> {
      deserializer.deserialize("{".getBytes());
    });

  }

}

class TestData {
  public byte[] bytes;
  public String name;
}
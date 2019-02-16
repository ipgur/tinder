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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class JsonTransformerTest {

  @Test
  public void testTransformer() {
    String string;
    JsonTransformer transformer = new JsonTransformer();

    string = transformer.render(new Object());
    Assertions.assertEquals("{}", string);

    string = transformer.render("abcd");
    Assertions.assertEquals("\"abcd\"", string);

    string = transformer.render(55L);
    Assertions.assertEquals("55", string);

    RandomBean bean = new RandomBean();
    bean.id = 999;
    bean.list = Arrays.asList("1", "äü");
    bean.decimal = new BigDecimal("0.1");
    string = transformer.render(bean);
    Assertions.assertEquals("{\"id\":999,\"list\":[\"1\",\"äü\"],\"decimal\":0.1}", string);
  }

}

class RandomBean {
  public long id;
  public List<String> list;
  public BigDecimal decimal;
}
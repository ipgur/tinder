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
package tinder.core.modules;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class TinderConfigurationTest {

  @Test
  public void testConfigurationDefaultValues() {
    // Keep this test to ensure that default values don't get changed naively
    TinderConfiguration configuration = ImmutableTinderConfiguration.builder()
        .build();

    Assertions.assertEquals(true, configuration.useServer());
    Assertions.assertEquals(8080, configuration.httpPort());
    Assertions.assertEquals("jdbi", configuration.jdbiInstanceName());
  }

}

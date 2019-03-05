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
package tinder.core.modules.metrics;

import com.timgroup.statsd.StatsDClient;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *
 * @author Raffaele Ragni
 */
public class StatsDHelperTest {

  @Test
  public void testHelper() {

    StatsDClient client = mock(StatsDClient.class);
    StatsDHelper sdh = new StatsDHelper(client);

    sdh.counterAround("metric_name", () -> null);
    verify(client, times(1)).increment("metric_name");

    sdh.timedAround("metric_name", () -> null);
    verify(client, times(1)).gauge(eq("metric_name"), anyLong());

  }

}

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

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.javalin.Context;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Raffaele Ragni
 */
public class HealthCheckRouteTest {

  @Test
  public void testHealthCheckOK() throws Exception {

    Context ctx = mock(Context.class);
    HealthCheckRegistry registry = mock(HealthCheckRegistry.class);

    SortedMap<String, HealthCheck.Result> results = new TreeMap<>();
    when(registry.runHealthChecks()).thenReturn(results);

    HealthCheckRoute route = new HealthCheckRoute(registry);

    // Empty results
    route.handle(ctx);
    // no healtchecks, always 200
    verify(ctx, times(1)).status(200);

    // One result
    results.put("ok", Result.healthy());
    route.handle(ctx);
    verify(ctx, times(2)).status(200);

    // Two results, but one is wrong
    results.put("ko", Result.unhealthy("Bad"));
    route.handle(ctx);
    verify(ctx, times(1)).status(500);
  }

}

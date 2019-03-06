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
import com.codahale.metrics.health.HealthCheckRegistry;
import io.javalin.Context;
import io.javalin.Handler;
import java.util.SortedMap;

/**
 *
 * @author Raffaele Ragni
 */
public class HealthCheckRoute implements Handler {

  final HealthCheckRegistry healthCheckRegistry;

  public HealthCheckRoute(HealthCheckRegistry healthCheckRegistry) {
    this.healthCheckRegistry = healthCheckRegistry;
  }

  @Override
  public void handle(Context ctx) throws Exception {

    ctx.header("Content-Type", "application/json");
    ctx.header("Cache-Control", "must-revalidate,no-cache,no-store");

    SortedMap<String, HealthCheck.Result> results = healthCheckRegistry.runHealthChecks();

    boolean anyFailed = results.entrySet()
        .stream()
        .map(e -> e.getValue().isHealthy())
        .filter(v -> v == false)
        .findAny()
        .isPresent();

    if (anyFailed) {
      ctx.status(500);
    } else {
      ctx.status(200);
    }

    ctx.json(results);
  }

}

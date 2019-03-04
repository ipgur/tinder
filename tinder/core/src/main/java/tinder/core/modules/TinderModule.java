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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import static java.util.Optional.of;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import spark.Response;
import spark.Spark;
import tinder.core.JDBILoader;
import tinder.core.modules.metrics.HealthCheckRoute;

/**
 *
 * @author Raffaele Ragni
 */
public class TinderModule {

  public static final String MDC_REQUEST_UUID = "request_uuid";

  public static final String HEADER_TINDER_REQUEST_UUID = "X-Tinder-RequestUUID";

  private static final Logger LOG = LoggerFactory.getLogger(TinderModule.class);

  final TinderConfiguration configuration;

  final MetricRegistry metricRegistry;
  final HealthCheckRegistry healthCheckRegistry;

  public TinderModule(TinderConfiguration configuration) {
    this.configuration = configuration;

    // Initialize registries.
    // These are always initialized, and they cost nothing to initialize, all thet start is a new
    // concurrent hash map inside at creation.
    LOG.info("Initializing metrics and healtcheck registries...");
    metricRegistry = new MetricRegistry();
    healthCheckRegistry = new HealthCheckRegistry();

    // Start up spark if enabled.
    if (configuration.useSpark()) {
      LOG.info("Starting Sparkjava...");
      Spark.port(configuration.sparkPort());
      Spark.threadPool(
          configuration.sparkMaxThreads(),
          configuration.sparkMinThreads(),
          configuration.sparkIdleTimeoutMillis());
      configuration.sparkStaticFilesLocation().ifPresent(Spark.staticFiles::location);
      // We always add the support for identifiable requests via the custom header
      Spark.before((req, resp) -> { requestUUIDFilterBefore(); });
      Spark.after((req, resp) -> { requestUUIDFilterAfter(resp); });
      // Map the healthchecks
      if (configuration.useHealtCheckEndpoint()) {
        LOG.info("Adding /healthcheck");
        Spark.get("/healthcheck", new HealthCheckRoute(healthCheckRegistry));
      }
    }

    // Register JMX reporter for metrics if enabled and have it start and go along.
    if (configuration.useJmxMetrics()) {
      LOG.info("Initializing JMX metrics reporter...");
      JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
      // Start it now
      reporter.start();
      // Make sure to terminate it at JVM end
      Runtime.getRuntime().addShutdownHook(new Thread(() -> reporter.stop()));
    }
  }

  public MetricRegistry metricRegistry() {
    return metricRegistry;
  }

  public HealthCheckRegistry healthCheckRegistry() {
    return healthCheckRegistry;
  }

  public TinderConfiguration configuration() {
    return configuration;
  }

  /**
   * Builds the default jdbi instance
   * @param configuration the app configuration.
   * @return jdbi instance
   */
  public Jdbi jdbi(TinderConfiguration configuration) {
    LOG.info("Returning JDBI instance {}", configuration.jdbiInstanceName());
    return JDBILoader.load(configuration.jdbiInstanceName(), of(metricRegistry()));
  }

  /**
   * Adds the request UUID header in responses.
   */
  static void requestUUIDFilterBefore() {
    String uuid = UUID.randomUUID().toString();
    MDC.put(MDC_REQUEST_UUID, uuid);
    LOG.info("Start of request UUID filter for request: {}", uuid);
  }

  /**
   * Adds the request UUID header in responses.
   */
  static void requestUUIDFilterAfter(Response resp) {
    String uuid = MDC.get(MDC_REQUEST_UUID);
    resp.header(HEADER_TINDER_REQUEST_UUID, uuid);
    LOG.info("End of request UUID filter for request: {}", uuid);
    MDC.remove(MDC_REQUEST_UUID);
  }

}

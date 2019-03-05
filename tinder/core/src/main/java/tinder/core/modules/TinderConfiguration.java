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

import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

/**
 *
 * @author Raffaele Ragni
 */
@Immutable
public interface TinderConfiguration {

  /**
   * Whether to bootstrap spark.
   * Change this only if you want to not open any http.
   * If you are building an offline component, put it to false
   * @return true if we want to use spark, default is true.
   */
  @Default default boolean useSpark() { return true; }

  /**
   * This is the port to listen for.
   * @return http(s) port to listen. Defaults to 8443 since secure() is defaulted to true.
   */
  @Default default int sparkPort() { return 8080; }

  /**
   * This is the min threads amount used by sparkjava.
   * @return default is -1 / unbound
   */
  @Default default int sparkMinThreads() { return -1; }

  /**
   * This is the max threads amount used by sparkjava.
   * @return default is -1 / unbound
   */
  @Default default int sparkMaxThreads() { return -1; }

  /**
   * This is the timeout for idle connections.
   * @return default is -1 / unbound
   */
  @Default default int sparkIdleTimeoutMillis() { return -1; }

  /**
   * Enables https.
   * You will require to pass along also the other options needed for this:
   *   - sparkKeystoreFile
   *   - sparkKeystorePassword
   * @return enables https
   */
  @Default default boolean sparkUseHttps() { return false; }

  /**
   * Where to find the Keystore file.
   * @return the Keystore file path
   */
  @Nullable String sparkKeystoreFile();

  /**
   * The password to the keystore.
   * @return the password to the keystore
   */
  @Nullable String sparkKeystorePassword();

  /**
   * Publish on the root http path, the static file path in this string.
   * Ex. specify here where the path is in the local resources.
   * @return the location of static files in the resources.
   */
  Optional<String> sparkStaticFilesLocation();

  /**
   * The prefix to use for sending statsd metrics.
   * default is "api"
   * @return the prefix for statsd
   */
  @Default default String statsDPrefix() { return "api"; }

  /**
   * The host of the statsd receiving agent.
   * Default is localhost.
   * @return the host for statsd target
   */
  @Default default String statsDHost() { return "localhost"; }

  /**
   * The post of the statsd receiving agent.
   * It uses UDP. Default is 8125.
   * @return port for statsd
   */
  @Default default int statsDPort() { return 8125; }

  /**
   * Configures if to setup JMX metrics.
   * The metric registry will be set up anyway, this is only to have aggregators for JMX retrieval.
   * @return if to setup JMX metrics
   */
  @Default default boolean useJmxMetrics() { return true; }

  /**
   * If to setup the /healthcheck endpoint (only works with useSpark = true).
   * @return if to setup the /healthcheck endpoint
   */
  @Default default boolean useHealtCheckEndpoint() { return true; }

  /**
   * The configuration name for the jdbi instance.
   * If you don't use jdbi, then just don't inject it via "@Inject JDBI Jdbi".
   * Default instance name is "jdbi"
   * The lookup will search for "name".[yaml|yml] both in the class path and in the same folder as the uber jar.
   * @return The jdbi instance.
   */
  @Default default String jdbiInstanceName() { return "jdbi"; }




}

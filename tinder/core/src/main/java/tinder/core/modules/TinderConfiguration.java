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
import java.util.function.Consumer;
import org.eclipse.jetty.server.Server;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

/**
 *
 * @author Raffaele Ragni
 */
@Immutable
public interface TinderConfiguration {

  /**
   * Whether to bootstrap the http server port.
   * Change this only if you want to not open any http.
   * If you are building an offline component, put it to false
   * @return true if we want to use http, default is true.
   */
  @Default default boolean useServer() { return true; }

  /**
   * Whether to only start the SSL port.
   * If false, it will also start unencrypted http.
   * @return true if we want to use ONLY https secured/SSL
   */
  @Default default boolean httpSSLOnly() { return true; }

  /**
   * This is the port to listen for.
   * @return http port to listen. Defaults to 8080
   */
  @Default default int httpPort() { return 8080; }

  /**
   * This is the port to listen for.
   * Keep in mind that http2 will work ONLY over SSL.
   * @return https port to listen. Defaults to 8443
   */
  @Default default int httpSSLPort() { return 8443; }

  /**
   * TPath to the keystore.
   * Default empty()
   * Keep in mind that http2 will work ONLY over SSL.
   * @return keystore file path
   */
  Optional<String> httpSSLKeystorePath();

  /**
   * Password for the keystore.
   * Default empty()
   * Keep in mind that http2 will work ONLY over SSL.
   * @return keystore password
   */
  Optional<String> httpSSLKeystorePassword();

  /**
   * A server customizer for jetty.
   * @return the customizer function a consumer of Server
   */
  @Default default Consumer<Server> httpServerConfigurator() { return s -> {}; }

  /**
   * Publish on the root http path, the static file path in this string.
   * Ex. specify here where the path is in the local resources.
   * @return the location of static files in the resources.
   */
  Optional<String> httpStaticFilesLocation();

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
   * If to setup the /healthcheck endpoint (only works with useHttp = true).
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

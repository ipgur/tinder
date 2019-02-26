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

import org.jdbi.v3.core.Jdbi;
import spark.Spark;
import tinder.core.JDBILoader;

/**
 *
 * @author Raffaele Ragni
 */
public class TinderModule {

  final TinderConfiguration configuration;

  public TinderModule(TinderConfiguration configuration) {
    this.configuration = configuration;
    if (configuration.useSpark()) {
      Spark.port(configuration.sparkPort());
      Spark.threadPool(
          configuration.sparkMaxThreads(),
          configuration.sparkMinThreads(),
          configuration.sparkIdleTimeoutMillis());
      configuration.sparkStaticFilesLocation().ifPresent(Spark.staticFiles::location);
    }
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
    return JDBILoader.load(configuration.jdbiInstanceName());
  }

}

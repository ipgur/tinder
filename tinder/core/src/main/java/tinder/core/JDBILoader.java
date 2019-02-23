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

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import static java.util.Optional.empty;
import java.util.Properties;
import java.util.function.Consumer;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads up the Jdbi instance.
 *
 * @author Raffaele Ragni
 */
public final class JDBILoader {

  private static final Logger LOG = LoggerFactory.getLogger(JDBILoader.class);

  private JDBILoader() {
  }

  /**
   * Creates a new Jdbi instance. Uses HikaryCP. Loads jdbi.yml default file. Use the load(String) for specifying
   * another file.
   *
   * @return Jdbi instance
   */
  public static Jdbi load() {
    return load("jdbi", empty(), empty(), empty());
  }

  /**
   * Creates a new Jdbi instance. Uses HikaryCP.
   *
   * @param name config name. This will lookup for name.y[a]ml in all known paths including the current folder
   * @return Jdbi instance
   */
  public static Jdbi load(String name) {
    return load(name, empty(), empty(), empty());
  }

  /**
   * Creates a new Jdbi instance. Uses HikaryCP. Loads jdbi.yml default file. Use the load(String) for specifying
   * another file.
   *
   * @param metricRegistry metric registry for the connection pool
   * @return Jdbi instance
   */
  public static Jdbi load(Optional<MetricRegistry> metricRegistry) {
    return load("jdbi", empty(), empty(), metricRegistry);
  }

  /**
   * Creates a new Jdbi instance. Uses HikaryCP.
   *
   * @param name config name. This will lookup for name.y[a]ml in all known paths including the current folder
   * @param metricRegistry metric registry for the connection pool
   * @return Jdbi instance
   */
  public static Jdbi load(String name, Optional<MetricRegistry> metricRegistry) {
    return load(name, empty(), empty(), metricRegistry);
  }

  /**
   * This signature is meant for user and password being passed while all the rest is loaded form config. Why this?
   * Because user and passwords are secrets and the application MAY want to load them dynamically and not statically
   * from a config file.
   *
   * @param name config name. This will lookup for name.y[a]ml in all known paths including the current folder
   * @param username dynamic username, if empty then the config file will be read for the username
   * @param password dynamic password, if empty then the config file will be read for the password
   * @param metricRegistry metric registry for the connection pool
   * @return Jdbi instance
   */
  public static Jdbi load(String name, Optional<String> username, Optional<String> password, Optional<MetricRegistry> metricRegistry) {

    HikariConfig config = new HikariConfig();

    // Function to load configurations and fill in to the hikari config, from a given path.
    // It only supports yaml.
    // Properties are hard coded: jdbcUrl, username, password
    // More props to come later..
    Consumer<Reader> configLoader = reader -> {
      Map<String, Object> values;
      Yaml yaml = new Yaml();
      values = yaml.load(reader);
      config.setJdbcUrl(values.get("jdbcUrl").toString());
      config.setUsername(username.orElse(values.get("username").toString()));
      config.setPassword(password.orElse(values.get("password").toString()));

      Map<String, Object> mapProps = (Map<String, Object>) values.get("properties");
      if (mapProps != null) {
        final Properties properties = new Properties();
        mapProps.entrySet().forEach((property) -> {
          properties.setProperty(property.getKey(), property.getValue().toString());
        });
        config.setDataSourceProperties(properties);
      }
    };

    LOG.info("Scanning for files with base name {}", name);

    // Look for configuration in the actual file system.
    Optional<Path> foundPath = Arrays.asList(
        Paths.get(name + ".yml"),
        Paths.get(name + ".yaml"),
        Paths.get(getJarPath(), name + ".yml"),
        Paths.get(getJarPath(), name + ".yaml")
    ).stream()
        .filter(Objects::nonNull)
        .filter(Files::exists)
        .findFirst();

    foundPath.ifPresent(path -> {
      LOG.debug("Reading from Paths: {}", path);
      withinReaderFromPath(path, reader -> configLoader.accept(reader));
    });

    // Look for configuration in the classpath / inside jar
    // In this special case some more 'manual' coding is required because classpaths are a special case.
    if (!foundPath.isPresent()) {
      InputStream is = null;
      try {
        is = ClassLoader.getSystemClassLoader().getResourceAsStream(name + ".yml");
        is = is == null ? ClassLoader.getSystemClassLoader().getResourceAsStream(name + ".yaml") : is;
        if (is != null) {
          LOG.debug("Reading from classpath");
          configLoader.accept(new InputStreamReader(is));
        }
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }

    HikariDataSource ds = new HikariDataSource(config);
    metricRegistry.ifPresent(m -> ds.setMetricRegistry(m));
    return Jdbi.create(ds);
  }

  private static String getJarPath() {
    return new File(toUri(JDBILoader.class.getProtectionDomain().getCodeSource().getLocation())).getPath();
  }

  /**
   * This utility is to readapt checked exceptions
   * @param url
   * @return
   */
  static void withinReaderFromPath(Path path, Consumer<Reader> fn) {
    try (Reader r = new FileReader(path.toFile())) { fn.accept(r); } catch (IOException ex) { throw new RuntimeException(ex); }
  }

  /**
   * This utility is to readapt checked exceptions
   * @param url
   * @return
   */
  static URI toUri(URL url) {
    try { return url.toURI(); } catch (URISyntaxException ex) { throw new RuntimeException(ex); }
  }

}

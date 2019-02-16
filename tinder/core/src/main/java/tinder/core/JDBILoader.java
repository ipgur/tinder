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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Optional;
import static java.util.Optional.empty;
import org.jdbi.v3.core.Jdbi;

/**
 * Loads up the Jdbi instance.
 *
 * @author Raffaele Ragni
 */
public class JDBILoader {

  /**
   * Creates a new Jdbi instance. Uses HikaryCP.
   *
   * @param name config name. This will lookup for <name>.y[a]ml in all known paths including the current folder
   * @return Jdbi instance
   */
  public static Jdbi load(String name) {
    return load(name, empty(), empty());
  }

  /**
   * This signature is meant for user and password being passed while all the rest is loaded form config. Why this?
   * Because user and passwords are secrets and the application MAY want to load them dynamically and not statically
   * from a config file.
   *
   * @param name config name. This will lookup for <name>.y[a]ml in all known paths including the current folder
   * @param username dynamic username, if empty then the config file will be read for the username
   * @param password dynamic password, if empty then the config file will be read for the password
   * @return Jdbi instance
   */
  public static Jdbi load(String name, Optional<String> username, Optional<String> password) {

    HikariConfig config = new HikariConfig();
    

    config.setJdbcUrl("jdbc:mysql://localhost:3306/db?zeroDateTimeBehavior=round");
    config.setUsername("user");
    config.setPassword("pass");
    config.addDataSourceProperty("dataSourceClassName", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
    config.addDataSourceProperty("autoCommit", "false");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("cachePrepStmts", "true");

    HikariDataSource ds = new HikariDataSource(config);

    return Jdbi.create(ds);
  }

}

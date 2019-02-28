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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.util.Optional.of;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class JdbiLoaderTest {

  @Test
  public void testJdbiLoaderConfig1() throws IOException {

    // Try the default file
    JDBILoader.load();

    // Try a different profile
    JDBILoader.load("config1");

    IllegalArgumentException ex;
    ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      // Try a file that does not exist
      JDBILoader.load("none");
    });
    Assertions.assertEquals("dataSource or dataSourceClassName or jdbcUrl is required.", ex.getMessage());

    // Test the loader on the workdir path
    Files.write(Paths.get("test2.yml"),
        ("jdbcUrl: jdbc:h2:mem:test;MODE\\=PostgreSQL;DB_CLOSE_DELAY\\=-1\n" +
        "username: sa\n" +
        "password: \"\"").getBytes());
    JDBILoader.load("test2");

    Files.delete(Paths.get("test2.yml"));
  }

  @Test
  public void testWithMetrics() {
    MetricRegistry reg = new MetricRegistry();
    JDBILoader.load(of(reg));
    JDBILoader.load("config1", of(reg));
  }

}

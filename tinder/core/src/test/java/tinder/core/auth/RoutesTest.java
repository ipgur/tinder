/*
 * Copyright 2019 rragni16.
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
package tinder.core.auth;

import java.util.Arrays;
import java.util.HashSet;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tinder.core.JDBILoader;

/**
 *
 * @author rragni16
 */
public class RoutesTest {
  @Test
  public void testRoutes() {
    Jdbi jdbi = JDBILoader.load();
    // Test with the spark endpoints now

    // Use random available port
    Spark.port(0);

    AuthenticationFilter.addAPIBasedFilter("/auth1/*", "http://localhost:4567");
    AuthenticationFilter.addAPIBasedFilter("/auth1-a/*", "http://localhost:4567", new HashSet(Arrays.asList("/myendpoint")));

    AuthenticationFilter.addJWTBasedFilter("/auth2/*", "seekrit");
    AuthenticationFilter.addJWTBasedFilter("/auth2-a/*", "seekrit", new HashSet(Arrays.asList("/myendpoint")));

    AuthenticationFilter.addDatabaseBasedFilter(jdbi, "/auth2/*");
    AuthenticationFilter.addDatabaseBasedFilter(jdbi, "/auth3/*", new HashSet(Arrays.asList("/myendpoint")));

    AuthenticationResources.addRegisterResource(jdbi);
    AuthenticationResources.addRegisterResource(jdbi, System.out::println);
    AuthenticationResources.addRegisterResource(jdbi, "/myRegistration");
    AuthenticationResources.addRegisterResource(jdbi, "/myRegistration2", System.out::println);

    AuthenticationResources.addLoginResource(jdbi);
    AuthenticationResources.addLoginResource(jdbi, "/myLogin");

    AuthenticationResources.addCheckTokenResource(jdbi);
    AuthenticationResources.addCheckTokenResource(jdbi, "/myChecktoken");

    // Make sure to not leave spark open here...
    Spark.stop();
  }
}

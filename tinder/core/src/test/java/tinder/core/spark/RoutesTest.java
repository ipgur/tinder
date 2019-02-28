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
package tinder.core.spark;

import java.util.Arrays;
import java.util.HashSet;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import spark.Spark;
import tinder.core.auth.AuthenticationFilter;
import tinder.core.auth.AuthenticationResources;
import tinder.core.modules.ImmutableTinderConfiguration;
import tinder.core.modules.TinderConfiguration;
import tinder.core.modules.TinderModule;

/**
 *
 * @author Raffaele Ragni
 *
 */
public class RoutesTest {
  @Test
  public void testRoutes() {

    // Keep all spark tests in this test, so that it won't clash with itself with the inner signleton.

    TinderConfiguration configuration = ImmutableTinderConfiguration.builder().build();
    TinderModule module = new TinderModule(configuration);
    Assertions.assertEquals(configuration, module.configuration());

    Jdbi jdbi = module.jdbi(configuration);

    // Test with the spark endpoints now

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

    AuthenticationResources.addJWTLoginResource(jdbi, "seekrit");
    AuthenticationResources.addJWTLoginResource(jdbi, "seekrit", "/myJWTLogin");

    AuthenticationResources.addCheckTokenResource(jdbi);
    AuthenticationResources.addCheckTokenResource(jdbi, "/myChecktoken");

    // Make sure to not leave spark open here...
    Spark.stop();
  }
}

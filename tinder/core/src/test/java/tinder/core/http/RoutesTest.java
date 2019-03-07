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
package tinder.core.http;

import com.codahale.metrics.health.HealthCheck;
import com.timgroup.statsd.StatsDClient;
import io.javalin.Javalin;
import java.util.Arrays;
import java.util.HashSet;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tinder.core.auth.AuthenticationFilter;
import tinder.core.auth.AuthenticationResources;
import tinder.core.modules.ImmutableTinderConfiguration;
import tinder.core.modules.JettyServerCreator;
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

    // Keep all http tests in this test, so that it won't clash with itself with the inner signleton.

    TinderConfiguration configuration = ImmutableTinderConfiguration.builder()
        .build();
    TinderModule module = new TinderModule(configuration);
    module.healthCheckRegistry().register("test", new HealthCheck(){
      @Override
      protected HealthCheck.Result check() throws Exception {
        return Result.healthy();
      }
    });
    StatsDClient sdc = module.statsDClient();
    Assertions.assertNotNull(sdc);

    Assertions.assertEquals(configuration, module.configuration());

    Jdbi jdbi = module.jdbi(configuration);

    // Test with the http endpoints now

    AuthenticationFilter filter = new AuthenticationFilter(module.javalin(), jdbi);

    filter.addAPIBasedFilter("/auth1/*", "http://localhost:4567");
    filter.addAPIBasedFilter("/auth1-a/*", "http://localhost:4567", new HashSet<>(Arrays.asList("/myendpoint")));

    filter.addJWTBasedFilter("/auth2/*", "seekrit");
    filter.addJWTBasedFilter("/auth2-a/*", "seekrit", new HashSet<>(Arrays.asList("/myendpoint")));

    filter.addDatabaseBasedFilter("/auth2/*");
    filter.addDatabaseBasedFilter("/auth3/*", new HashSet<>(Arrays.asList("/myendpoint")));

    AuthenticationResources ar = new AuthenticationResources(module.javalin(), jdbi);

    ar.addRegisterResource();
    ar.addRegisterResource("/my-registration");
    ar.addRegisterResource("/my-registration2", System.out::println);

    ar.addLoginResource();
    ar.addLoginResource("/my-login");

    ar.addCheckTokenResource();
    ar.addCheckTokenResource("/my-checktoken");

    AuthenticationResources ar2 = new AuthenticationResources(Javalin.create(), jdbi);
    ar2.addRegisterResource(System.out::println);
    ar2.addJWTLoginResource("seekrit");
    ar2.addJWTLoginResource("seekrit", "/my-jwt-login");

    // We actually want to test that even with providing a wrong path and password, until the actual startup happens
    // nothing throws an error.

    new JettyServerCreator(ImmutableTinderConfiguration.builder()
        .httpSSLOnly(false)
        .httpSSLKeystorePath("a")
        .httpSSLKeystorePassword("b")
        .useJmxMetrics(false)
        .useHealtCheckEndpoint(false)
        .build());

  }

}

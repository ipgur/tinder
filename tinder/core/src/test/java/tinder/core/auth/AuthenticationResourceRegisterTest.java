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
package tinder.core.auth;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import spark.Request;
import spark.Response;
import spark.Spark;
import tinder.core.JDBILoader;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationResourceRegisterTest {

  @Test
  public void testRegister() throws DatabaseException, LiquibaseException {
    Jdbi jdbi = JDBILoader.load();
    AuthenticationResources.upgradeByLiquibase(jdbi);

    Request req = mock(Request.class);
    Response resp = mock(Response.class);

    // This user is automatically confirmed because we are passing an empty() callback of the verification code consumer
    when(req.body()).thenReturn("{\"email\": \"my.email@mail.go\", \"password\": \"12345678\"}");
    AuthenticationResources.register(jdbi, empty(), req, resp);

    // This user won't be enabled because we just print the confirmation code
    when(req.body()).thenReturn("{\"email\": \"my.emai2@mail.go\", \"password\": \"12345678\"}");
    AuthenticationResources.register(jdbi, of(System.out::println), req, resp);

    jdbi.withHandle(h -> {
      boolean enabled1 = h.createQuery("select enabled from tinder_users where email = :email")
          .bind("email", "my.email@mail.go")
          .mapTo(Boolean.class)
          .findOnly();
      boolean enabled2 = h.createQuery("select enabled from tinder_users where email = :email")
          .bind("email", "my.emai2@mail.go")
          .mapTo(Boolean.class)
          .findOnly();
      Assertions.assertTrue(enabled1);
      Assertions.assertFalse(enabled2);
      return null;
    });

    // Test with the spark endpoints now

    // Use random available port
    Spark.port(0);

    AuthenticationResources.addRegisterResource(jdbi);
    AuthenticationResources.addRegisterResource(jdbi, System.out::println);
    AuthenticationResources.addRegisterResource(jdbi, "/myRegistration");
    AuthenticationResources.addRegisterResource(jdbi, "/myRegistration2", System.out::println);

    // Make sure to not leave spark open here...
    Spark.stop();
  }

}

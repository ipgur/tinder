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

import java.util.UUID;
import liquibase.exception.LiquibaseException;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import spark.Request;
import spark.Response;
import spark.Spark;
import tinder.core.JDBILoader;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationResourceCheckTokenTest {

  @Test
  public void testCheckToken() throws LiquibaseException {
    Jdbi jdbi = JDBILoader.load();
    AuthenticationResources.upgradeByLiquibase(jdbi);

    Request req = mock(Request.class);
    Response resp = mock(Response.class);

    // Test a valid token (a 1h token)
    String validToken = UUID.randomUUID().toString();
    addToken(jdbi, validToken, 3600_000L);
    when(req.headers("Authorization")).thenReturn("Bearer " + validToken);
    String email = AuthenticationResources.checkToken(jdbi, req, resp);
    Assertions.assertEquals("\"email\"", email);
    verify(resp).status(200);

    // Test an invalid token, save oen and pick another completely random one
    validToken = UUID.randomUUID().toString();
    addToken(jdbi, validToken, 3600_000L);
    when(req.headers("Authorization")).thenReturn("Bearer " + UUID.randomUUID().toString());
    AuthenticationResources.checkToken(jdbi, req, resp);
    verify(resp, times(1)).status(401);

    // Test an invalid header
    when(req.headers("Authorization")).thenReturn("aaaa not a token");
    AuthenticationResources.checkToken(jdbi, req, resp);
    verify(resp, times(2)).status(401);

    // Test an expired token (1h ago)
    String expiredToken = UUID.randomUUID().toString();
    addToken(jdbi, expiredToken, -3600_000L);
    when(req.headers("Authorization")).thenReturn("Bearer " + expiredToken);
    AuthenticationResources.checkToken(jdbi, req, resp);
    verify(resp, times(3)).status(401);

    // Test with the spark endpoints now

    // Use random available port
    Spark.port(0);

    AuthenticationResources.addCheckTokenResource(jdbi);
    AuthenticationResources.addCheckTokenResource(jdbi, "/myChecktoken");

    // Make sure to not leave spark open here...
    Spark.stop();
  }

  private void addToken(Jdbi jdbi, String token, long deltaTime) {
    jdbi.withHandle(h -> {
      h.execute("insert into tinder_tokens(token, email, expiration) "
          + "values(?, ?, DATEADD(MILLISECOND, "+deltaTime+", CURRENT_TIMESTAMP))",
          token, "email");
      return null;
    });
  }

}

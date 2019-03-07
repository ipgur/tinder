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

import io.javalin.Context;
import io.javalin.HttpResponseException;
import io.javalin.Javalin;
import java.util.UUID;
import liquibase.exception.LiquibaseException;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tinder.core.JDBILoader;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationResourceCheckTokenTest {

  @Test
  public void testCheckToken() throws LiquibaseException {
    Jdbi jdbi = JDBILoader.load();
    AuthenticationResources ar = new AuthenticationResources(mock(Javalin.class), jdbi);
    ar.upgradeByLiquibase();

    Context ctx = mock(Context.class);

    // Test a valid token (a 1h token)
    String validToken = UUID.randomUUID().toString();
    addToken(jdbi, validToken, 3600_000L);
    when(ctx.header("Authorization")).thenReturn("Bearer " + validToken);

    AuthenticationResources.checkToken(jdbi, ctx);
    verify(ctx).json("email");
    verify(ctx).status(200);

    // Test an invalid token, save oen and pick another completely random one
    validToken = UUID.randomUUID().toString();
    addToken(jdbi, validToken, 3600_000L);
    when(ctx.header("Authorization")).thenReturn("Bearer " + UUID.randomUUID().toString());
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationResources.checkToken(jdbi, ctx);
    });

    // Test an invalid header
    when(ctx.header("Authorization")).thenReturn("aaaa not a token");
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationResources.checkToken(jdbi, ctx);
    });

    // Test an expired token (1h ago)
    String expiredToken = UUID.randomUUID().toString();
    addToken(jdbi, expiredToken, -3600_000L);
    when(ctx.header("Authorization")).thenReturn("Bearer " + expiredToken);
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationResources.checkToken(jdbi, ctx);
    });

    // Test with null header
    when(ctx.header("Authorization")).thenReturn(null);
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationResources.checkToken(jdbi, ctx);
    });
  }

  public static void addToken(Jdbi jdbi, String token, long deltaTime) {
    jdbi.withHandle(h -> {
      h.execute("insert into tinder_tokens(token, email, expiration) "
          + "values(?, ?, DATEADD(MILLISECOND, "+deltaTime+", CURRENT_TIMESTAMP))",
          token, "email");
      return null;
    });
  }

}

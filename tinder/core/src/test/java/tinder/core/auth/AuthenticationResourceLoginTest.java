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
import io.javalin.Javalin;
import io.jsonwebtoken.security.Keys;
import java.math.BigInteger;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
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
public class AuthenticationResourceLoginTest {

  @Test
  public void testLogin() throws LiquibaseException {
    Jdbi jdbi = JDBILoader.load();
    AuthenticationResources ar = new AuthenticationResources(mock(Javalin.class), jdbi);
    ar.upgradeByLiquibase();

    // Add a sample user
    jdbi.withHandle(h -> {
      h.execute("insert into tinder_users(email, hash, enabled) values(?, ?, ?)",
        "testlogin@test.bb",
        new String(AuthenticationResources.hashAndClean("12345678".toCharArray())),
        1);
      return null;
    });

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn("{\"email\": \"testlogin@test.bb\", \"password\": \"12345678\"}");
    AuthenticationResources.login(jdbi, ctx);

    String token = jdbi.withHandle(h -> {
      return h.createQuery("select token from tinder_tokens where email = :email")
        .bind("email", "testlogin@test.bb")
        .mapTo(String.class)
        .findOnly();
    });
    Assertions.assertNotNull(token);

    // Try a bad login
    when(ctx.body()).thenReturn("{\"email\": \"testlogin@test.bb\", \"password\": \"aaa\"}");
    AuthenticationResources.login(jdbi, ctx);
    verify(ctx).status(401);
  }

  @Test
  public void testJWTLogin() throws LiquibaseException {
    SecureRandom random = new SecureRandom();
    String secret = new BigInteger(500, random).toString(32);
    byte[] keyBytes = secret.getBytes();
    SecretKey key = Keys.hmacShaKeyFor(keyBytes);

    Jdbi jdbi = JDBILoader.load();
    AuthenticationResources ar = new AuthenticationResources(mock(Javalin.class), jdbi);
    ar.upgradeByLiquibase();

    // Add a sample user
    jdbi.withHandle(h -> {
      h.execute("insert into tinder_users(email, hash, enabled) values(?, ?, ?)",
        "testlogin@test.cc",
        new String(AuthenticationResources.hashAndClean("12345678".toCharArray())),
        1);
      return null;
    });

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn("{\"email\": \"testlogin@test.cc\", \"password\": \"12345678\"}");
    AuthenticationResources.loginJWT(jdbi, secret, ctx);

    // Try a bad login
    when(ctx.body()).thenReturn("{\"email\": \"testlogin@test.cc\", \"password\": \"aaa\"}");
    AuthenticationResources.loginJWT(jdbi, secret, ctx);
    verify(ctx).status(401);
  }

}

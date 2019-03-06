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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import static java.util.Optional.empty;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tinder.core.auth.AuthenticationFilter.REQ_EMAIL;
import tinder.core.helpers.GsonSerializer;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationFilterJWTTest {

  @Test
  public void testJWT() {
    SecureRandom random = new SecureRandom();
    String secret = new BigInteger(500, random).toString(32);

    Context ctx = mock(Context.class);

    byte[] keyBytes = secret.getBytes();
    SecretKey key = Keys.hmacShaKeyFor(keyBytes);

    String jws;
    jws = Jwts.builder()
        .serializeToJsonWith(new GsonSerializer<>())
        .setSubject("user@ameil.com")
        .signWith(key)
        .compact();

    when(ctx.header(any())).thenReturn("Bearer "+ jws);
    when(ctx.path()).thenReturn("/someendpoint");

    AuthenticationFilter.authenticateJTWFilter(secret, empty(), ctx);
    verify(ctx).attribute(REQ_EMAIL, "user@ameil.com");

    // Use an expired token
    jws = Jwts.builder()
        .serializeToJsonWith(new GsonSerializer<>())
        .setExpiration(Date.from(Instant.now().minusMillis(60000)))
        .setSubject("user@ameil.com")
        .signWith(key)
        .compact();
    when(ctx.header(any())).thenReturn("Bearer "+ jws);
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationFilter.authenticateJTWFilter(secret, empty(), ctx);
    });

    // Use an invalid token
    when(ctx.header(any())).thenReturn("notabearer");
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationFilter.authenticateJTWFilter(secret, empty(), ctx);
    });
    when(ctx.header(any())).thenReturn("Bearer blahblah");
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationFilter.authenticateJTWFilter(secret, empty(), ctx);
    });

    // Use a different signature...
    jws = Jwts.builder()
        .serializeToJsonWith(new GsonSerializer<>())
        .setSubject("user@ameil.com")
        .signWith(key)
        .compact();
    when(ctx.header(any())).thenReturn("Bearer "+ jws);
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationFilter.authenticateJTWFilter(new BigInteger(500, random).toString(32),
          empty(), ctx);
    });

    // Thest the skipping of filtering
    when(ctx.path()).thenReturn("/login");
    AuthenticationFilter.authenticateJTWFilter(secret, empty(), ctx);
  }

}

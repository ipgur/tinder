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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import java.util.Set;
import javax.crypto.SecretKey;
import org.jdbi.v3.core.Jdbi;
import retrofit2.Retrofit;
import spark.Request;
import spark.Response;
import spark.Spark;
import tinder.core.helpers.GsonDeserializer;

/**
 * Applies authentication filtering to the current sparkjava set.
 * It comes with some default API designs, but remains parametric with endpoint
 * URIs and filter patterns.
 * The basic back end implementation is what remains mostly unchanged.
 *
 * authentication: filters
 *
 * AuthenticationFilter.addDatabaseBasedFilter()
 * - will filter all requests and check db for the Bearer token.
 * - can specify which requests to filter or to exclude although a default
 *   signature will be there (ex, excluding /login /register...)
 * - option and parameters: table name and/or column name for tokens
 *
 * AuthenticationFilter.addAPIBasedFilter()
 * - Same applying filter and options as before...
 * - but with added lambda function to be implemented by dev that will call the
 *   auth api
 * - also provide a basic impl that calls another api that was build up by this
 *   own library in the /check default endpoint, or customize the url but at
 *   least implement the protocol because the jsons will be the same.
 *
 * These filters automatically have a default ignore path list:
 * - /login
 * - /register
 * - /checktoken
 * These are not checked against authentication for obvious reasons, everything
 * else will be locked by authentication.
 * That's the default. It can be changed by user.
 * (optional list in filter methods?)
 *
 * @author Raffaele Ragni
 */
public final class AuthenticationFilter {

  public static final String REQ_EMAIL = "email";
  public static final String REQ_USER = REQ_EMAIL;

  private AuthenticationFilter() {
  }

  /**
   * Adds a filter that will check for authorization header and bearer tokens according to the implementation already
   * covering those aspects in the AuthenticationResources.
   * This implementation is used when both the login and filter are in the same API, so that both access to the same
   * authentication database.
   * If you are building a model of multiple satellite APIs that contact a centralized authentication API, use the other
   * implementation addAPIBasedFilter().
   * @param jdbi the jdbi instance of the database we want to use.
   * @param filterPath the filter Path where the authentication will be checked. can use wildcards *
   */
  public static void addDatabaseBasedFilter(Jdbi jdbi, String filterPath) {
    addDatabaseBasedFilter(jdbi, filterPath, empty());
  }

  /**
   * Variant with excluding endpoints
   * @param jdbi the jdbi instance of the database we want to use.
   * @param excludeEndpoints which endpoints to exclude from this filter.
   * @param filterPath the filter Path where the authentication will be checked. can use wildcards *
   */
  public static void addDatabaseBasedFilter(Jdbi jdbi, String filterPath, Set<String> excludeEndpoints) {
    addDatabaseBasedFilter(jdbi, filterPath, of(excludeEndpoints));
  }

  /**
   * Variant with excluding endpoints
   * @param jdbi the jdbi instance of the database we want to use.
   * @param excludeEndpoints which endpoints to exclude from this filter.
   * @param filterPath the filter Path where the authentication will be checked. can use wildcards *
   */
  public static void addDatabaseBasedFilter(Jdbi jdbi, String filterPath, Optional<Set<String>> excludeEndpoints) {
    Spark.before(filterPath, (req, resp) -> authenticateDatabaseFilter(jdbi, excludeEndpoints, req, resp));
  }

  static void authenticateDatabaseFilter(Jdbi jdbi, Optional<Set<String>> excludeEndpoints, Request req, Response resp) {
    if (!shouldApplyFilter(excludeEndpoints, req)) {
      // Don't check authentication for the exclusion list.
      return;
    }

    // Here we are more or less taking the same implementation in /checktoken because we are doing the direct
    // database bypass case.
    // The main difference is that this is a filter and only alters the repsonse in case of 401 unauthenticated.
    // Most of the code will look the same.
    String authHeader = req.headers("Authorization");
    authHeader = authHeader == null ? "" : authHeader.trim();

    // Can't accept other token types, at least not in this implementaion
    // devs can implement their own if they want
    if (!authHeader.toLowerCase().startsWith("Bearer".toLowerCase())) {
      Spark.halt(401, "Only bearer tokens are supported");
    }

    String token = authHeader.substring("Bearer".length()).trim();
    // Just a simple find-email and return 200 or 401 and the email itself as string (maybe json later?)
    // It is important to return the email because the calling api must know who is the owner of that token to profile
    // permissioning in their own api
    Optional<String> email = jdbi.withHandle(h -> {
      return h.createQuery("select email from tinder_tokens where token = :token and expiration > :stamp")
        .bind("token", token)
        .bind("stamp", Instant.now())
        .mapTo(String.class)
        .findFirst();
    });

    if (!email.isPresent()) {
      Spark.halt(401, "Unauthorized");
    }
  }

  public static void addAPIBasedFilter(String filterPath, String apiBaseURL) {
    addAPIBasedFilter(filterPath, apiBaseURL, empty());
  }

  public static void addAPIBasedFilter(String filterPath, String apiBaseURL, Set<String> excludeEndpoints) {
    addAPIBasedFilter(filterPath, apiBaseURL, of(excludeEndpoints));
  }

  public static void addAPIBasedFilter(String filterPath, String apiBaseURL, Optional<Set<String>> excludeEndpoints) {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(apiBaseURL)
        .build();
    AuthenticationService service = retrofit.create(AuthenticationService.class);
    Spark.before(filterPath, (req, resp) -> authenticateAPIFilter(service, excludeEndpoints, req, resp));
  }

  static void authenticateAPIFilter(AuthenticationService service, Optional<Set<String>> excludeEndpoints, Request req, Response resp) {
    if (!shouldApplyFilter(excludeEndpoints, req)) {
      // Don't check authentication for the exclusion list.
      return;
    }

    // Basically just forward the call using the same authorization header.
    String email = service.checkUrl(req.headers("Authorization"));
    // Add it to the req attribute if not null
    if (email != null) {
      req.attribute(REQ_EMAIL, email);
    } else {
      Spark.halt(401, "Unauthorized");
    }
  }

  //
  // JWT based authentication filtering
  //

  /**
   * Adds a filter that will check a JWT token instead of a UUID token.
   * JTW token are signature based, so all you need is the server secret in the configuration.
   * This eliminates the need to database look ups for validity and expiration.
   * @param filterPath the path where to install the filter, ex. "/authenticated/*"
   * @param secret the secret used to sign or verify the JWT (in this case, to verify)
   */
  public static void addJWTBasedFilter(String filterPath, String secret) {
    addJWTBasedFilter(filterPath, secret, empty());
  }

  /**
   * Adds a filter that will check a JWT token instead of a UUID token.
   * JTW token are signature based, so all you need is the server secret in the configuration.
   * This eliminates the need to database look ups for validity and expiration.
   * @param filterPath the path where to install the filter, ex. "/authenticated/*"
   * @param excludeEndpoints which endpoints to exclude from this filter.
   * @param secret the secret used to sign or verify the JWT (in this case, to verify)
   */
  public static void addJWTBasedFilter(String filterPath, String secret, Set<String> excludeEndpoints) {
    addJWTBasedFilter(filterPath, secret, of(excludeEndpoints));
  }

  /**
   * Adds a filter that will check a JWT token instead of a UUID token.
   * JTW token are signature based, so all you need is the server secret in the configuration.
   * This eliminates the need to database look ups for validity and expiration.
   * @param filterPath the path where to install the filter, ex. "/authenticated/*"
   * @param excludeEndpoints which endpoints to exclude from this filter.
   * @param secret the secret used to sign or verify the JWT (in this case, to verify)
   */
  public static void addJWTBasedFilter(String filterPath, String secret, Optional<Set<String>> excludeEndpoints) {
    Spark.before(filterPath, (req, resp) -> authenticateJTWFilter(secret, excludeEndpoints, req, resp));
  }

  static void authenticateJTWFilter(String secret, Optional<Set<String>> excludeEndpoints, Request req, Response resp) {
    if (!shouldApplyFilter(excludeEndpoints, req)) {
      // Don't check authentication for the exclusion list.
      return;
    }

    String authHeader = req.headers("Authorization");
    authHeader = authHeader == null ? "" : authHeader.trim();

    // Can't accept other token types, at least not in this implementaion
    // devs can implement their own if they want
    if (!authHeader.toLowerCase().startsWith("Bearer".toLowerCase())) {
      Spark.halt(401, "Only bearer tokens are supported");
    }

    String token = authHeader.substring("Bearer".length()).trim();

    try {
      byte[] keyBytes = secret.getBytes();
      SecretKey key = Keys.hmacShaKeyFor(keyBytes);
      Claims body = (Claims) Jwts.parser()
        .deserializeJsonWith(new GsonDeserializer<>())
        .setSigningKey(key)
        .parse(token)
        .getBody();
      String email = body.getSubject();
      req.attribute(REQ_EMAIL, email);
      return;
    } catch (ExpiredJwtException e) {
      Spark.halt(401, "JWT is expired");
    } catch (UnsupportedJwtException | MalformedJwtException e) {
      Spark.halt(401, "Not a valid JWT");
    } catch (SignatureException e) {
      Spark.halt(401, "Not authorized");
    }

    Spark.halt(401, "Not authorized");
  }

  //
  // Some common parts...
  //

  // Checks if the filter should be applied based on the exclusion list, or the default auth endpoints.
  static boolean shouldApplyFilter(Optional<Set<String>> excludeEndpoints, Request req) {
    // Thera are some well known endpoints that we must exclude from this filter.
    // They are user customizable too, but we have defaults to the defaults of the AuthenticationResources.
    Set<String> exclusions = excludeEndpoints.orElse(new HashSet<>(Arrays.asList(
        "/register",
        "/login",
        "/checktoken",
        "/"
    )));

    boolean toSkip = exclusions.stream()
        .filter(s -> req.uri().equals(s))
        .findFirst()
        .isPresent();

    return ! toSkip;
  }

}
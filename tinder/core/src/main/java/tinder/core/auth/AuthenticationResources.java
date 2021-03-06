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

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.javalin.Context;
import io.javalin.HttpResponseException;
import io.javalin.Javalin;
import io.javalin.json.JavalinJson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;
import static java.util.Collections.emptyMap;
import java.util.Date;
import java.util.Optional;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import java.util.UUID;
import java.util.function.Consumer;
import javax.crypto.SecretKey;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tinder.core.helpers.GsonSerializer;

/**
 * Authentication endpoint resources.
 *
 * This tool will install resources for authentication, based on which ones
 * are requested via the addXX methods.
 *
 * This tool may be used embedded in your API if it is self standing, but also
 * you can build a central authentication API from this and then rely on the
 * check endpoint from satellite APIs, to verify tokens.
 *
 * Main endpoints being provided:
 *
 * /register
 * addRegisterResource(path with default to /register,
 *      code {... what to do with confirmation code})
 * - this one accepts email+password and created the entry in the db,
 *   and returns a confirmation code to activate the user later.
 * - the developer will deal with sending codes via email/sms whatever (lambda)
 *
 * /register/verify?code=XXX
 * this to be called once via maybe a GET link from an email:
 * AuthenticationResources.confirm(email, code)
 * - confirms and enable a user, updates the db with a user to be enabled this
 *   way (post registration process finalization)
 * Make a resource endpoint or just leave the dev to implement this?
 *
 * /login
 * addLoginResource(path with default to login)
 * - adds the login endpoint, JSONs and protocol is only the one from the
 *   provided layer, else the developer has to implement all by himself.
 * - accepts a JSON with {email: "", password: ""} for login.
 * - returns a JSON with the user profile and 200 OK if login was correct.
 *
 * /checktoken
 * addCheckTokenResource(path with default to /checktoken)
 * - checks if a token is valid or not, this is used if you want to build an
 *   auth api and rely on that, by using the addAPIBasedFilter() from another
 *   upstream api.
 *   should return all the possible user info as a json when all is correct.
 *   it does not do profilation but that doesn't mean you can add a /profile
 *   endpoint yourself and use a addDatabaseBasedFilter() in the main
 *   auth/central api.
 *
 * @author Raffaele Ragni
 */
public final class AuthenticationResources {

  private static final String PREFIX_AUTH = "Auth :: ";
  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationResources.class);

  private final Javalin javalin;
  private final Jdbi jdbi;

  public AuthenticationResources(Javalin javalin, Jdbi jdbi) {
    this.javalin = javalin;
    this.jdbi = jdbi;
  }

  // 30 minutes
  private static final long DEFAULT_TOKEN_EXPIRE_MS = 1800_000L;

  //
  // Setup helpers
  //

  // This path is hard coded so careful if you move that resource away...
  // Also the contents of that xml must be taken care of as this class will depend on table and columns names in there.
  private static final String AUTH_CHANGELOG = "tinder/core/auth/tinder-auth-liquibase.xml";

  /**
   * Creates all the necessary tables needed for the user and authentication implementation of this class.
   * This operation is incremental as liquibase keeps track of DDL changes.
   * @throws liquibase.exception.DatabaseException
   */
  public void upgradeByLiquibase() throws DatabaseException, LiquibaseException {
    jdbi.withHandle(h -> {
      LOG.info(PREFIX_AUTH+"Upgrading auth tables through liquibase");
      h.begin();
      Connection con = h.getConnection();
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(con));
      Liquibase liquibase = new liquibase.Liquibase(AUTH_CHANGELOG, new ClassLoaderResourceAccessor(), database);
      liquibase.update(new Contexts(), new LabelExpression());
      h.commit();
      return null;
    });
  }

  //
  // Resource: /register
  //

  /**
   * Adds the registration endpoint to '/register'.
   */
  public void addRegisterResource() {
    addRegisterResource(empty(), empty());
  }

  /**
   * Adds the registration endpoint to '/register'.
   * @param resourcePath a different path where to register the endpoint
   */
  public void addRegisterResource(String resourcePath) {
    addRegisterResource(of(resourcePath), empty());
  }

  /**
   * Adds the registration endpoint to '/register'.
   * @param verificationCodeConsumer the functional consumer for sending the verification code to the user.
   */
  public void addRegisterResource(Consumer<String> verificationCodeConsumer) {
    addRegisterResource(empty(), of(verificationCodeConsumer));
  }

  /**
   * Adds the registration endpoint to '/register'.
   * @param resourcePath a different path where to register the endpoint
   * @param verificationCodeConsumer the functional consumer for sending the verification code to the user.
   */
  public void addRegisterResource(String resourcePath, Consumer<String> verificationCodeConsumer) {
    addRegisterResource(of(resourcePath), of(verificationCodeConsumer));
  }

  /**
   * Adds the registration endpoint to '/register'.
   *
   * @param resourcePath the path for the registration endpoint or empty for default (/register)
   * @param verificationCodeConsumer
   *        the consumer that will send the verification code to the user, or empty for always accept the user code upon
   *        registration (WARNING: it is recommended to implement a verification method and not leave it on auto accept,
   *        for security reasons)
   */
  public void addRegisterResource(Optional<String> resourcePath, Optional<Consumer<String>> verificationCodeConsumer) {
    String resPath = resourcePath.orElse("/register");
    LOG.info(PREFIX_AUTH+"Adding resource {}", resPath);
    javalin.post(resPath, c -> register(jdbi, verificationCodeConsumer, c));
  }

  /**
   * Confirms a user activation after registration.
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   * @param email the email of the user (it's the primary key)
   * @param code the verification code
   */
  public static void confirm(Jdbi jdbi, String email, String code) {
    jdbi.withHandle(h -> {
      h.execute("update tinder_users set enabled = true where email = ? and verification = ?", email, code);
      return null;
    });
  }

  // Handle this as default package level so we can mock/use it later in unit tests
  static String register(Jdbi jdbi, Optional<Consumer<String>> verificationCodeConsumer, Context ctx) {
    ImmutableLoginData loginData = JavalinJson.fromJson(ctx.body(), ImmutableLoginData.class);

    // Create the hash+salt and remove password from memory immediately.
    char[] hash = hashAndClean(loginData.password().toCharArray());
    String code = UUID.randomUUID().toString();

    jdbi.withHandle(h -> {
      // Email is primary key so if a user registers again this will throw an error.
      // enabled is 0 by default from the liquibase schema.
      h.execute("insert into tinder_users(email, hash, verification) values(?, ?, ?)",
          loginData.email(), new String(hash), code);
      return null;
    });

    Consumer<String> callback = verificationCodeConsumer.orElse(c -> confirm(jdbi, loginData.email(), c));
    callback.accept(code);

    return "";
  }

  // Handle this as default package level so we can mock/use it later in unit tests
  // Make a hash+salt and clean the char[] for the password, so that memory is also cleaned from it.
  static char[] hashAndClean(char[] password) {
    char[] hashed = BCrypt.withDefaults().hashToChar(12, password);
    // Delete the password
    Arrays.fill(password, ' ');
    return hashed;
  }

  //
  // Login resource
  //

  public void addLoginResource() {
    addLoginResource(empty());
  }

  public void addLoginResource(String resourcePath) {
    addLoginResource(of(resourcePath));
  }

  public void addLoginResource(Optional<String> resourcePath) {
    String resPath = resourcePath.orElse("/login");
    LOG.info(PREFIX_AUTH+"Adding resource {}, UUID token version", resPath);
    javalin.post(resPath, c -> login(jdbi, c));
  }

  static void login(Jdbi jdbi, Context ctx) {
    ImmutableLoginData loginData = JavalinJson.fromJson(ctx.body(), ImmutableLoginData.class);

    Optional<String> optHash = jdbi.withHandle(h -> {
      // Finds the hash code of the user but only if enabled. Returned as optional
      return h.createQuery("select hash from tinder_users where email = :email and enabled = true")
          .bind("email", loginData.email())
          .mapTo(String.class)
          .findFirst();
    });

    boolean loggedIn = optHash
        .map(hash -> BCrypt
            .verifyer()
            .verify(loginData.password().toCharArray(), hash.toCharArray())
            .verified)
        .orElse(false);

    // Unauthorized, login didn't succeed.
    if (!loggedIn) {
      ctx.status(401);
      return;
    }

    // Create a new token with expiration default.
    String token = UUID.randomUUID().toString();
    Instant expiresAt = jdbi.withHandle(h -> {
      h.execute("insert into tinder_tokens(token, email, expiration) "
          // It's important to rely on the UTC always, and Instant does that.
          + "values(?, ?, ?)",
          token, loginData.email(), Instant.now().plusMillis(DEFAULT_TOKEN_EXPIRE_MS));
      return h.createQuery("select expiration from tinder_tokens where token = :token")
          .bind("token", token)
          .mapTo(Instant.class)
          .findOnly();
    });

    ImmutableTokenResult tokenResult = ImmutableTokenResult.builder()
        .token(token)
        // constant as long as we use the DEFAULT_TOKEN_EXPIRE_MS which is 30 minutes
        .expiresIn("30m")
        .expiresAt(expiresAt.toString())
        .build();

    ctx.json(tokenResult);
  }

  //
  // Variant login: returns JWT, if you are going to use the JWT filter, you need to use this one for login instead.
  //

  /**
   * Sets up a login endpoint that handles JTW type of tokens being returned.
   * By default it maps to "/login"
   * @param secret the secret used to sign JWT. Keep it private and only on the server side.
   */
  public void addJWTLoginResource(String secret) {
    addJWTLoginResource(secret, empty());
  }

  /**
   * Sets up a login endpoint that handles JTW type of tokens being returned.
   * @param secret the secret used to sign JWT. Keep it private and only on the server side.
   * @param resourcePath the path of the resource to bind.
   */
  public void addJWTLoginResource(String secret, String resourcePath) {
    addJWTLoginResource(secret, of(resourcePath));
  }

  public void addJWTLoginResource(String secret, Optional<String> resourcePath) {
    String resPath = resourcePath.orElse("/login");
    LOG.info(PREFIX_AUTH+"Adding resource {}, JWT version", resPath);
    javalin.post(resPath, c -> loginJWT(jdbi, secret, c));
  }

  static void loginJWT(Jdbi jdbi, String secret, Context ctx) {
    ImmutableLoginData loginData = JavalinJson.fromJson(ctx.body(), ImmutableLoginData.class);

    Optional<String> optHash = jdbi.withHandle(h -> {
      // Finds the hash code of the user but only if enabled. Returned as optional
      return h.createQuery("select hash from tinder_users where email = :email and enabled = true")
          .bind("email", loginData.email())
          .mapTo(String.class)
          .findFirst();
    });

    boolean loggedIn = optHash
        .map(hash -> BCrypt
            .verifyer()
            .verify(loginData.password().toCharArray(), hash.toCharArray())
            .verified)
        .orElse(false);

    // Unauthorized, login didn't succeed.
    if (!loggedIn) {
      ctx.status(401);
      return;
    }

    Instant expiresAt = Instant.now().plusMillis(DEFAULT_TOKEN_EXPIRE_MS);
    byte[] keyBytes = secret.getBytes();
    SecretKey key = Keys.hmacShaKeyFor(keyBytes);
    String token = Jwts.builder()
        .serializeToJsonWith(new GsonSerializer<>())
        .setSubject(loginData.email())
        .setExpiration(Date.from(expiresAt))
        .signWith(key)
        .compact();

    ImmutableTokenResult tokenResult = ImmutableTokenResult.builder()
        .token(token)
        // constant as long as we use the DEFAULT_TOKEN_EXPIRE_MS which is 30 minutes
        .expiresIn("30m")
        .expiresAt(expiresAt.toString())
        .build();

    ctx.json(tokenResult);
  }

  //
  // Check resource
  //

  public void addCheckTokenResource() {
    addCheckTokenResource(empty());
  }

  public void addCheckTokenResource(String resourcePath) {
    addCheckTokenResource(of(resourcePath));
  }

  public void addCheckTokenResource(Optional<String> resourcePath) {
    String resPath = resourcePath.orElse("/checktoken");
    LOG.info(PREFIX_AUTH+"Adding resource {}", resPath);
    javalin.post(resPath, c -> checkToken(jdbi, c));
  }

  static void checkToken(Jdbi jdbi, Context ctx) {
    String authHeader = ctx.header("Authorization");
    authHeader = authHeader == null ? "" : authHeader.trim();

    // Can't accept other token types, at least not in this implementaion
    // devs can implement their own if they want
    if (!authHeader.toLowerCase().startsWith("Bearer".toLowerCase())) {
      throw new HttpResponseException(401, "Only bearer tokens are supported", emptyMap());
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

    if (email.isPresent()) {
      ctx.status(200);
      ctx.json(email.get());
    } else {
      throw new HttpResponseException(401, "", emptyMap());
    }
  }

}

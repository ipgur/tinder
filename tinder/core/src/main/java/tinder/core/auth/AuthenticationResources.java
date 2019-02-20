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
import java.sql.Connection;
import java.util.Arrays;
import java.util.Optional;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import java.util.UUID;
import java.util.function.Consumer;
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
import spark.Request;
import spark.Response;
import spark.Spark;
import tinder.core.JsonTransformer;

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
public class AuthenticationResources {

  //
  // Setup helpers
  //

  // This path is hard coded so careful if you move that resource away...
  // Also the contents of that xml must be taken care of as this class will depend on table and columns names in there.
  private static final String AUTH_CHANGELOG = "tinder/core/auth/tinder-auth-liquibase.xml";

  /**
   * Creates all the necessary tables needed for the user and authentication implementation of this class.
   * This operation is incremental as liquibase keeps track of DDL changes.
   * @param jdbi the jdbi instance of the database we want to use.
   * @throws liquibase.exception.DatabaseException
   */
  public static void upgradeByLiquibase(Jdbi jdbi) throws DatabaseException, LiquibaseException {
    jdbi.withHandle(h -> {
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
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   */
  public static void addRegisterResource(Jdbi jdbi) {
    addRegisterResource(jdbi, empty(), empty());
  }

  /**
   * Adds the registration endpoint to '/register'.
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   * @param resourcePath a different path where to register the endpoint
   */
  public static void addRegisterResource(Jdbi jdbi, String resourcePath) {
    addRegisterResource(jdbi, of(resourcePath), empty());
  }

  /**
   * Adds the registration endpoint to '/register'.
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   * @param verificationCodeConsumer the functional consumer for sending the verification code to the user.
   */
  public static void addRegisterResource(Jdbi jdbi, Consumer<String> verificationCodeConsumer) {
    addRegisterResource(jdbi, empty(), of(verificationCodeConsumer));
  }

  /**
   * Adds the registration endpoint to '/register'.
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   * @param resourcePath a different path where to register the endpoint
   * @param verificationCodeConsumer the functional consumer for sending the verification code to the user.
   */
  public static void addRegisterResource(Jdbi jdbi, String resourcePath, Consumer<String> verificationCodeConsumer) {
    addRegisterResource(jdbi, of(resourcePath), of(verificationCodeConsumer));
  }

  /**
   * Adds the registration endpoint to '/register'.
   *
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   * @param resourcePath the path for the registration endpoint or empty for default (/register)
   * @param verificationCodeConsumer
   *        the consumer that will send the verification code to the user, or empty for always accept the user code upon
   *        registration (WARNING: it is recommended to implement a verification method and not leave it on auto accept,
   *        for security reasons)
   */
  public static void addRegisterResource(Jdbi jdbi, Optional<String> resourcePath, Optional<Consumer<String>> verificationCodeConsumer) {
    Spark.post(resourcePath.orElse("/register"), (req, resp) -> register(jdbi, verificationCodeConsumer, req, resp));
  }

  /**
   * Confirms a user activation after registration.
   * @param jdbi the jdbi instance of the database containing the user and authentication tables.
   * @param email the email of the user (it's the primary key)
   * @param code the verification code
   */
  public static void confirm(Jdbi jdbi, String email, String code) {
    jdbi.withHandle(h -> {
      h.execute("update tinder_users set enabled = 1 where email = ? and verification = ?", email, code);
      return null;
    });
  }

  // Handle this as default package level so we can mock/use it later in unit tests
  static Response register(Jdbi jdbi, Optional<Consumer<String>> verificationCodeConsumer, Request req, Response resp) {
    JsonTransformer tr = new JsonTransformer();
    ImmutableLoginData loginData = tr.parse(req.body(), ImmutableLoginData.class);

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

    return resp;
  }

  // Handle this as default package level so we can mock/use it later in unit tests
  // Make a hash+salt and clean the char[] for the password, so that memory is also cleaned from it.
  static char[] hashAndClean(char[] password) {
    char[] hashed = BCrypt.withDefaults().hashToChar(12, password);
    // Delete the password
    Arrays.fill(password, ' ');
    return hashed;
  }

}

package api;

import api.healthchecks.APIHealthCheck;
import api.resources.Example;
import api.services.ConfigReloader;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.javalin.Javalin;
import javax.inject.Inject;
import javax.inject.Named;
import liquibase.exception.LiquibaseException;
import org.jdbi.v3.core.Jdbi;
import tinder.core.auth.AuthenticationFilter;
import tinder.core.auth.AuthenticationResources;

public class App {

  @Inject Jdbi jdbi;
  @Inject Example example;
  @Inject ConfigReloader configReloader;
  @Inject @Named("jwt_secret") String secret;
  @Inject HealthCheckRegistry healthCheckRegistry;
  @Inject Javalin javalin;

  @Inject
  public App() {
  }

  public static void main(String[] args) {
    DaggerAppComponent.create().app();
  }

  @Inject public void postConstruct() {
    try {

      // Add the extra parts after all endpoints are installed

      healthCheckRegistry.register("jdbi", new APIHealthCheck(jdbi));

      AuthenticationFilter filter = new AuthenticationFilter(javalin, jdbi);
      filter.addJWTBasedFilter("/auth/*", secret);

      javalin.start();

      AuthenticationResources ar = new AuthenticationResources(javalin, jdbi);

      ar.upgradeByLiquibase();
      ar.addRegisterResource();
      ar.addJWTLoginResource(secret);

    } catch (LiquibaseException ex) {
      throw new RuntimeException(ex);
    }
  }
}

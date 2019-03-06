package api;

import api.healthchecks.APIHealthCheck;
import api.resources.Example;
import api.services.ConfigReloader;
import com.codahale.metrics.health.HealthCheckRegistry;
import javax.inject.Inject;
import javax.inject.Named;
import liquibase.exception.LiquibaseException;
import org.jdbi.v3.core.Jdbi;
import tinder.core.auth.AuthenticationResources;

public class App {

  @Inject Jdbi jdbi;
  @Inject Example example;
  @Inject ConfigReloader configReloader;
  @Inject @Named("jwt_secret") String secret;
  @Inject HealthCheckRegistry healthCheckRegistry;

  @Inject
  public App() {
  }

  public static void main(String[] args) {
    DaggerAppComponent.create().app();
  }

  @Inject public void postConstruct() {
    try {

      // Add the extra parts after all endpoints are installed

      AuthenticationResources.upgradeByLiquibase(jdbi);
      AuthenticationResources.addRegisterResource(jdbi);
      AuthenticationResources.addJWTLoginResource(jdbi, secret);

      healthCheckRegistry.register("jdbi", new APIHealthCheck(jdbi));

    } catch (LiquibaseException ex) {
      throw new RuntimeException(ex);
    }
  }
}

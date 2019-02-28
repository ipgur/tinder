package api;

import api.resources.Example;
import api.services.ConfigReloader;
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

  @Inject
  public App() {
  }

  public static void main(String[] args) {
    DaggerAppComponent.create().app();
  }

  @Inject public void postConstruct() {
    try {

      AuthenticationResources.upgradeByLiquibase(jdbi);

      AuthenticationFilter.addJWTBasedFilter("/auth/*", secret);

      AuthenticationResources.addRegisterResource(jdbi);
      AuthenticationResources.addJWTLoginResource(jdbi, secret);

    } catch (LiquibaseException ex) {
      throw new RuntimeException(ex);
    }
  }
}

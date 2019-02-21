package api;

import api.resources.Example;
import api.services.ConfigReloader;
import javax.inject.Inject;
import liquibase.exception.LiquibaseException;
import org.jdbi.v3.core.Jdbi;
import static spark.Spark.staticFiles;
import tinder.core.auth.AuthenticationResources;

public class App {

  @Inject Jdbi jdbi;

  @Inject Example example;

  @Inject ConfigReloader configReloader;

  @Inject
  public App() {
    // Swagger will be available on the root, by static files in docs resources.
    staticFiles.location("/docs");
  }

  @Inject public void postConstruct() {
    try {
      AuthenticationResources.upgradeByLiquibase(jdbi);
      AuthenticationResources.addRegisterResource(jdbi);
      AuthenticationResources.addLoginResource(jdbi);
      AuthenticationResources.addCheckTokenResource(jdbi);
    } catch (LiquibaseException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void main(String[] args) {
    DaggerAppComponent.create().app();
  }

}

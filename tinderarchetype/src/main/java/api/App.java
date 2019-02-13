package api;

import api.resources.Example;
import api.services.ConfigReloader;
import javax.inject.Inject;
import static spark.Spark.staticFiles;

public class App {

  @Inject Example example;
  
  @Inject ConfigReloader configReloader;

  @Inject
  public App() {
    // Swagger will be available on the root, by static files in docs resources.
    staticFiles.location("/docs");
  }

  public static void main(String[] args) {
    DaggerAppComponent.create().app();
  }

}

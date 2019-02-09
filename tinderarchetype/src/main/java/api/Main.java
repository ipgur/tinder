package api;

import api.resources.Example;
import api.resources.ResourceExample;
import static spark.Spark.staticFiles;

public class Main {
  public static void main(String[] args) {
    // Swagger will be available on the root, by static files in docs resources.
    staticFiles.location("/docs");
    // Bind all your generated resources here...
    // Patter is Resource<Name>.bind(new <Name>());
    ResourceExample.bind(new Example());
  }
}

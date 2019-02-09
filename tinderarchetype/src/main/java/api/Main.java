package api;

import api.resources.Example;
import api.resources.ResourceExample;
import static spark.Spark.staticFiles;

public class Main {
  public static void main(String[] args) {
    staticFiles.location("/docs");
    ResourceExample.bind(new Example());
  }
}

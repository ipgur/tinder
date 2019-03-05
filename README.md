
# Tinder API

Build status: [![CircleCI](https://circleci.com/gh/raffaeleragni/tinder.svg?style=svg)](https://circleci.com/gh/raffaeleragni/tinder)

## Tinder is an API stack.

It's based on a similar idea of Dropwizard. It even uses most of the same libraries. What's different from that one then?

* Jersey is replaced by Sparkjava
* HK2 is replaced by Dagger2
* Jackson is replaced by gson

The dependencies are small and lean, and easy to maintain without too many complicated interconnections. Check it out on the  [sample diagram](docs/dependencies_example.png).
It relies on source code generation, and inert runtimes. What does that mean? Tinder doesn't do runtime magic, it does runtime science.

* No class path scanning annotations, all is generated via APT tools.
* No hard troubleshooting and debugging of complicated production issues that can't be reproduced because the runtime DI and tools are too dynamic.
* Compile time safety of bindings and injections (Using Dagger 2, for example.)

## How to try it

* Check out this repository,
* do a `./mvnw install` from the `tinder` folder
* do a `./mvnw package` from the `tinderarchetype` folder
* run the sample with `java -jar target/app.jar` from the `tinderarchetype` folder.
* alternatively you can start it up via docker `docker run -it --rm -p 8080:8080 archetype/tinder-archetype:1.1-SNAPSHOT`
* open browser on http://localhost:8080/

The archetype provided takes care already of building the swagger.yml and the swagger-ui, and also for building a docker image out of it (all based on maven plugins).

The main dependency used is `tinder-core`, however `tinder-processors `is also added as an APT step.

## How to start your application

Simply checkout the `tinderarchetype` and use it as a template for starting up. As for the core libraries that are required, they should be already public on the maven central. You can also use local built snapshots in case of need (check if pom uses snapshots, if so then you need to build tinder first), since the archetype is shading the final uber jar (only 5mb!). You can also retag and push the docker image built in the maven package step.

From there on you can just go and play with it.

Performance tests done using wrk and a medium class laptop resulted in handling 37000 requests per second.

## Features implemented by Tinder

On the APT processors:
 * @Scheduling on top of a class that needs periodic activation
   * @PeriodicallyScheduled on the method that needs to be periodically scheduled
 * JAX-RS annotations that bind to Sparkjava, Supported annotations are:
   * @Resource to mark a resource, this annotation comes from tinder.
   * @Path
   * @GET, @POST, @PUT, @PATCH, @DELETE
   * @PathParam, @QueryParam, @@headerParam in parameters
   * parameters in path specified by "{}" (conversion is made on source generation)

On the configuration:
 * Extend the TinderModule as shown in the archetype,
 * Configuring some sparkjava parameters (https to come)
 * Setup of a jdbi instance by default
 * Setup for /healthcheck endpoint
 * Implementations available for authentication filtering and endpoints (/register, /login, /checktoken) and JWT choice of implementation.

## Some examples

A simple api resource.

```java
@Resource
@Path("/")
public class Example {
  public Example() {
    // This is the binding of the resource, you can decide to do it in different manners
    // here we choose to go via constructor just for simplicity of the example.
    // ResourceExample is the generated class via APT, they are in the form of Resource<name>
    ResourceExample.bind(this);
  }
  @POST
  @Path("/echo/")
  public String echo(String input) {
    return input;
  }
}
```

Setting up authentication filters, endpoints and a healtcheck.

```java
  public static void main(String[] args) {
    ...

    // using liquibase to create/update database tables.
    // not needed normally if you manage them yourself.
    AuthenticationResources.upgradeByLiquibase(jdbi);

    // specifying a path that needs authentication
    // since the /register and /login endpoints are in the root, it's best if
    // you do not use a root wildcard, even if the class knows how to ignore them.
    AuthenticationFilter.addJWTBasedFilter("/auth/*", secret);

    // add the /register
    AuthenticationResources.addRegisterResource(jdbi);
    // add the /login
    AuthenticationResources.addJWTLoginResource(jdbi, secret);

    // A custom healthcheck (APIHealthCheck is custom, not part of tinder core)
    healthCheckRegistry.register("jdbi", new APIHealthCheck(jdbi));
  }
```



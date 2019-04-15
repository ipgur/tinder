
# Tinder API

Name     | Status |
-------- | ------ |
Build    | [![CircleCI](https://circleci.com/gh/raffaeleragni/tinder.svg?style=svg)](https://circleci.com/gh/raffaeleragni/tinder) |
Coverage | [![codecov](https://codecov.io/gh/raffaeleragni/tinder/branch/master/graph/badge.svg)](https://codecov.io/gh/raffaeleragni/tinder) |


## Tinder is an API stack.

It's based on a similar idea of Dropwizard. It even uses most of the same libraries. What's different from that one then?

* Jersey is replaced by Javalin
* HK2 is replaced by Dagger2

The dependencies are small and lean, and easy to maintain without too many complicated interconnections. Check it out on the  [sample diagram](docs/dependencies_example.png).
It relies on source code generation, and inert runtimes. What does that mean? Tinder doesn't do runtime magic, it does runtime science.

* No class path scanning annotations, all is generated via APT tools.
* No hard troubleshooting and debugging of complicated production issues that can't be reproduced because the runtime DI and tools are too dynamic.
* Compile time safety of bindings and injections (Using Dagger 2, for example.)

## How to try it

* Check out this repository,
* do a `./mvnw install` from the `tinder` folder
* do a `./mvnw package` from the `tinderarchetype` folder
* run the sample with `java -jar target/original-app.jar` from the `tinderarchetype` folder.
* alternatively you can start it up via docker `docker run -it --rm -p 8080:8080 archetype/tinder-archetype:1.1-SNAPSHOT`
* open browser on http://localhost:8080/ (if httpSSLOnly(false)) or https://localhost:8443/
* Eventually you should provide your own keystore.jks for SSL

The archetype provided takes care already of building the swagger.yml and the swagger-ui, and also for building a docker image out of it (all based on maven plugins).

The main dependency used is `tinder-core`, however `tinder-processors `is also added as an APT step.

## How to start your application

Simply checkout the `tinderarchetype` and use it as a template for starting up. As for the core libraries that are required, they should be already public on the maven central. You can also use local built snapshots in case of need (check if pom uses snapshots, if so then you need to build tinder first), since the archetype is shading the final uber jar (for about a 20mb final). You can also retag and push the docker image built in the maven package step.

From there on you can just go and play with it.

Performance tests done using wrk and a medium class laptop resulted in handling 37000 requests per second. Enabling stdout logging may decrease such performance.

```
$ wrk -c 64 -d 30s http://localhost:8080/test/1
Running 30s test @ http://localhost:8080/test/1
  2 threads and 64 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     4.33ms   16.24ms 259.45ms   95.61%
    Req/Sec    19.06k     6.77k   27.23k    78.28%
  1129369 requests in 30.06s, 238.03MB read
Requests/sec:  37567.43
Transfer/sec:      7.92MB
```


## Features implemented by Tinder

On the APT processors:
 * @Scheduling on top of a class that needs periodic activation
   * @PeriodicallyScheduled on the method that needs to be periodically scheduled
 * JAX-RS annotations that bind to javalin, Supported annotations are:
   * @Resource to mark a resource, this annotation comes from tinder.
   * @Path
   * @GET, @POST, @PUT, @PATCH, @DELETE
   * @PathParam, @QueryParam, @HeaderParam in parameters
   * parameters in path specified by "{}" (conversion is made on source generation)

On the configuration:
 * Extend the TinderModule as shown in the archetype,
 * Configuring some javalin parameters + https
 * Setup of a jdbi instance by default
 * Setup for /healthcheck endpoint
 * Implementations available for authentication filtering and endpoints (/register, /login, /checktoken) and JWT choice of implementation.

## Some examples

A simple api resource.
Uses JAX-RS annotations.
Not all annotations are supported, see the list above.

```java
@Resource
@Path("/")
public class Example {
  @Inject public Example(Javalin javalin) {
    // This is the binding of the resource, you can decide to do it in different manners
    // here we choose to go via constructor just for simplicity of the example.
    // ResourceExample is the generated class via APT, they are in the form of Resource<name>
    ResourceExample.bind(javalin, this);
  }
  @POST
  @Path("/echo/{v}")
  public String echo(String input, @PathParam("v") Integer v) {
    return input + v;
  }
  // You can also use javalin Context directly, they will be recognized and passed as is.
  @PUT
  @Path("/raw")
  public String raw(Context ctx) {
    return "";
  }
}
```

Setting up authentication filters, endpoints and a healtcheck.

```java
  public static void main(String[] args) {
    ...
    AuthenticationResources ar = new AuthenticationResources(javalin, jdbi);
    // using liquibase to create/update database tables.
    // not needed normally if you manage them yourself.
    ar.upgradeByLiquibase();

    // add the /register
    ar.addRegisterResource();
    // add the /login
    ar.addJWTLoginResource(secret);

    AuthenticationFilter filter = new AuthenticationFilter(javalin, jdbi);
    // specifying a path that needs authentication
    // since the /register and /login endpoints are in the root, it's best if
    // you do not use a root wildcard, even if the class knows how to ignore them.
    filter.addJWTBasedFilter("/auth/*", secret);

    // A custom healthcheck (APIHealthCheck is custom, not part of tinder core)
    healthCheckRegistry.register("jdbi", new APIHealthCheck(jdbi));
  }
```


## Authentication features

Tinder API implements some authentication for you. Those are simple registration, login and filtering endpoints.
The recommended implementation to use is JWT, as it does not require databse lookups for token validation.
At the current moment JTW is best used in the monolythic setup, that is, you have one api with register & login also in the same API.
You could setup JWT via satellite, but then it needs to be setup with the same secret across all APIs using it.

JWT secret can be changed in case of breach with little consequance: all it means upon change / restart of the APIs is just that tokens need to be reaquired.

One alternative is to use UUID generated tokens. Those are done via SecureRandom (java implementation) and they are random enough for
security purposes. This means that your filter (API kind of filter) will query the 'central' auth api where the /checktoken is.
Because of the nature of UUID, the /checktoken must return the email (user PK), which in the JWT case is embedded in the JWT.

JWT setup:

```java
// Upgrade/create the database tables required by authentication.
// Uses liquibase, only incremental changes.
authenticationResources.upgradeByLiquibase();

// Add a filter on /auth/* (best not use /* for exclusion reasons)
// This is a JWT filter, all it needs is the secret to verify the signature.
authenticationFilter.addJWTBasedFilter("/auth/*", secret);

// Add a /register resource POST that needs a {"email": "...", "password": "..."}
// you can pass a function that will receive the generated confirmation code and send
// it to the user via mail / use the consumer signature
authenticationResources.addRegisterResource(confirmationCode -> ... send email to user...);

// Or use no consumer function, but then that means NO confirmation flow is enabled!
authenticationResources.addRegisterResource();

// Setup a /login endpoint, same data as register, that returns a JWT token.
// JWT tokens are 30 minutes valid by default
authenticationResources.addJWTLoginResource(secret);

...

// Later you can make an endpoint that accepts the confirmationCode
@GET
@Path("/user/{user}/confirm/{code}")
public String confirm(@PathParam("code") String user, @PathParam("code") String code) {}
  // This is the final confirmation, and will enable the user for logins.
  AuthenticationFilter.confirm(jdbi, email, code);
  return "";
}
```

Alternative UUID token based:

```java
// Filter checks in DB directly for token
authenticationFilter.addDatabaseBasedFilter("/auth/*");
// Login returns UUID token directly
authenticationResources.addLoginResource();
```

Alternative using remote API
```java
// On the remote api
authenticationResources.addCheckTokenResource();
// On your api
authenticationFilter.addAPIBasedFilter("/auth/*", "https://your.login.api/checktoken");
```

In case of filters, the user is stored in `req.attribute(AuthenticationResources.REQ_EMAIL)`.

Of course you can implement your own authentication too. The usage of these endpoint initalizers is totally optional, and if not used they are totally inactive.


## Metrics and healthcheck features

Three main things are available here:

 * classic 'healthchecks'
 * classic 'metrics'
 * a statsd client (DD implementation)

We will skip the classic metrics in this example and start with the healthchecks.

### Healthchecks

To define a healthcheck, just do it as usual when using the codahale metrics.
Example:

```java
public class APIHealthCheck extends HealthCheck {

  final Jdbi jdbi;

  public APIHealthCheck(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  protected Result check() throws Exception {
    return jdbi.withHandle(h -> {
      try {
        Integer result = h.select("select 1").mapTo(Integer.class).findOnly();
        if (result == 1) {
          return Result.healthy();
        } else {
          return Result.unhealthy("result was not 1");
        }
      } catch (RuntimeException e) {
        return Result.unhealthy(e);
      }
    });
  }

}
```

Then just add it to the provided (via dagger2) module isntance / injected component.

```java
@Inject HealthCheckRegistry healthCheckRegistry;

...

healthCheckRegistry.register("jdbi", new APIHealthCheck(jdbi));
```

How to setup the instance / component? That is shown in the AppModule:

```java
@Module
public class AppModule extends TinderModule {

  public AppModule() {
    super(ImmutableTinderConfiguration.builder()
      // Add your configuration here...
      .httpStaticFilesLocation("/docs")
      .httpSSLOnly(false)
      .httpSSLKeystorePath(AppModule.class.getResource("/keystore.jks").toString())
      .httpSSLKeystorePassword("changeit")
      .build());
  }

  @Provides
  @Singleton
  public HealthCheckRegistry getHealthCheckRegistry() {
    return healthCheckRegistry();
  }

  @Provides
  @Singleton
  public Javalin getJavalin() {
    return javalin();
  }

  ...
}
```

And AppComponent / App

```java
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
  App app();
}

```

```java
public class App {

  @Inject HealthCheckRegistry healthCheckRegistry;

  @Inject
  public App() {
  }

  public static void main(String[] args) {
    DaggerAppComponent.create().app();
  }

  @Inject public void postConstruct() {
    healthCheckRegistry.register("jdbi", new APIHealthCheck(jdbi));
  }
}
```

### Metrics via statsd

Just as above you can obtain the component for metrics / statsd in the same manner. In the AppModule:

```java
  @Provides
  @Singleton
  public StatsDClient getStatsDClient() {
    return statsDClient();
  }

  @Provides
  @Singleton
  public StatsDHelper getStatsDHelper() {
    return new StatsDHelper(statsDClient());
  }
```

Then using it:

```java
  @Inject StatsDHelper sdh;

  @GET
  @Path("/test")
  public String testdb() {
    return sdh.timedAround("request_endpoint_test", () -> {
      // do your thing?
    });
  }
```

## Note: Why the name Tinder

Initially Tinder was using Sparkjava as a container. The name was directly related to tinder and spark words, seems self explanatory. I only realized later there are already platforms or projects with such name. But after all, nowadays you can't name anything originally anymore. Later on Tinder moved to Javalin because that's what Sparkjava evolved into. It was a little hard to follow such eovlution as it was mentioned mostly on github issues that Javalin is the Sparkjava '3' reimplementation.

Nevertheless, the name still remains as the group/artifact ids are now uploaded, but since I were asked more than once about the origin of the name, I thought it would make sense to write this down.

RR

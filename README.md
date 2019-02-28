
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
* open browser on http://localhost:8080/

The archetype provided takes care already of building the sqagger.yml and the swagger-ui, and also for building a docker image out of it (all based on maven plugins).

The main dependency used is `tinder-core`, however `tinder-processors `is also added as an APT step.

## How to start your application

Simply checkout the `tinderarchetype` and use it as a template for starting up. As for the core libraries that are required, they should be already public on the maven central. You can also use local built snapshots in case of need (check if pom uses snapshots, if so then you need to build tinder first), since the archetype is shading the final uber jar (only 5mb!). You can also retag and push the docker image built in the maven package step.

From there on you can just go and play with it.

Performance tests done using wrk and a medium class laptop resulted in handling 37000 requests per second.

## Features implemented by Tinder

On the APT processors:
 * @Scheduled
 * JAX-RS annotations

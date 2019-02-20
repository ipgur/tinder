
# Tinder API

Build status: [![CircleCI](https://circleci.com/gh/raffaeleragni/tinder.svg?style=svg)](https://circleci.com/gh/raffaeleragni/tinder)

## Tinder is an API stack.

It's based on a similar idea of Dropwizard. It even uses most of the same libraries. What's different from that one then?

* Jersey is replaced by Sparkjava
* HK2 is replaced by Dagger2
* Jackson is replaced by gson

The dependencies are small and lean, and easy to maintain without too many complicated interconnections. Check it out on the  ![sample diagram](docs/dependencies_example.png).
It relies on source code generation, and inert runtimes. What does that mean? Tinder doesn't do runtime magic, it does runtime science.

* No class path scanning annotations, all is generated via APT tools.
* No hard troubleshooting and debugging of complicated production issues that can't be reproduced because the runtime DI and tools are too dynamic.
* Compile time safety of bindings and injections (Using Dagger 2, for example.)





/*
 * Copyright 2018 Raffaele Ragni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package api.services;

import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.SECONDS;
import tinder.core.PeriodicallyScheduled;
import tinder.core.Scheduling;

/**
 *
 * @author Raffaele Ragni
 */
// You need this annotation on top of your scheduling class.
// This will create a 1:1 class with a thread pool of 1 ready to schedule your methods
@Scheduling
public class MyThing {

  // If you need access to it for stopping, you can obtain it from the start().
  // Or just rely on jvm termination and ignore this, since this is optional.
  private final ScheduledExecutorService scheduler;

  public MyThing() {
    // You're the one passing the instance to the start(), which means you don't necessarily needs a default constructor
    // in your class. You can use initializing DI frameworks, constructors with parameters and so on.
    scheduler = ScheduledMyThing.start(this);
  }

  // A sample 1 second poll.
  // By default scheduled methods are immediately executed for the first go.
  // The default unit of time (also as per IS) is seconds. You can change it via unit.
  @PeriodicallyScheduled(1)
  public void doit() {
    System.out.println("fixed delay, starts immediately");
  }

  // This is the case when you don't want to wait before running the 2nd runs.
  // waitBeforeStarting = false will trigger the method from start to start, as opposed from end to start in the default
  // value.
  @PeriodicallyScheduled(value = 1, waitBeforeRestart = false)
  public void doit2() {
    System.out.println("fixed rate, starts immediately");
  }

  // An example on how to stop the scheduler after some time from the start.
  // immediateStart = false makes sure that the method is not called immediately on the first go, so it goes into
  // waiting from the first moment.
  @PeriodicallyScheduled(value = 10, unit = SECONDS, immediateStart = false)
  public void stopAll() {
    System.out.println("delayed start (always of same wait walue)");
    scheduler.shutdown();
  }

}

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
package tinder.core;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Triggers the method and waits for some time before doing it again.
 * This is equivalent of ScheduledExecutorService.scheduleWithFixedDelay
 * @author Raffaele Ragni
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface PeriodicallyScheduled {

  /**
   * The amount to wait between job triggering.
   * @return
   */
  long value() default 0;

  /**
   * Time unit of the value, defaults to SECONDS.
   * @return
   */
  TimeUnit unit() default SECONDS;

  /**
   * Waits for the current run before counting time forward, or just ignore it and always go with clock.
   * This determines the difference between delay and rate.
   * When true, it waits the amount of time between job end and job start.
   * When false, it waits the amount of time between job start and job start.
   * @return
   */
  boolean waitBeforeRestart() default true;

  /**
   * Either starts immediately then true, or wait the same amount before going the first time when false.
   * @return
   */
  boolean immediateStart() default true;
}

package tinder.processors;

import tinder.core.PeriodicallyScheduled;
import tinder.core.Scheduling;

/*
 * Copyright 2019 Raffaele Ragni.
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


/**
 *
 * @author Raffaele Ragni
 */
@Scheduling
public class ScheduledProcessorTarget {

  @PeriodicallyScheduled(1)
  public void oneSecond() {
    System.out.println("give me one second...");
  }

}

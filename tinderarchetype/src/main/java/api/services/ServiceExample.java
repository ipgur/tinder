package api.services;

import javax.inject.Inject;
import tinder.core.PeriodicallyScheduled;
import tinder.core.Scheduling;

@Scheduling
public class ServiceExample {

  @Inject
  public ServiceExample() {
    ScheduledServiceExample.start(this);
  }

  @PeriodicallyScheduled(10)
  public void printIt() {
    System.out.println("10 seconds");
  }
}

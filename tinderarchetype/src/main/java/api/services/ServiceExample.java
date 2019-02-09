package api.services;

import javax.inject.Inject;
import tinder.core.Scheduling;

@Scheduling
public class ServiceExample {

  @Inject
  public ServiceExample() {
    ScheduledServiceExample.start(this);
  }
}

package api.services;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tinder.core.PeriodicallyScheduled;
import tinder.core.Scheduling;

@Scheduling
public class ServiceExample {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceExample.class);
  
  @Inject
  public ServiceExample() {
    ScheduledServiceExample.start(this);
  }

  @PeriodicallyScheduled(10)
  public void printIt() {
    LOG.debug("Periodic scheduled: check every 10 seconds");
  }
}

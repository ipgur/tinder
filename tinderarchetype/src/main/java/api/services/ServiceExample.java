package api.services;

import api.db.dao.SampleDAO;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tinder.core.PeriodicallyScheduled;
import tinder.core.Scheduling;

@Scheduling
public class ServiceExample {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceExample.class);

  @Inject SampleDAO sampleDAO;

  @Inject
  public ServiceExample() {
    ScheduledServiceExample.start(this);
  }

  @PeriodicallyScheduled(10)
  public void printIt() {
    LOG.debug("Periodic scheduled: check every 10 seconds");
  }

  public String getIt() {
    return sampleDAO.test();
  }
}

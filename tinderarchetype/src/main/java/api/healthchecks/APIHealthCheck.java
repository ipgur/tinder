
package api.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.jdbi.v3.core.Jdbi;

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

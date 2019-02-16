package api.db.dao;

import javax.inject.Inject;
import org.jdbi.v3.core.Jdbi;

/**
 *
 * @author Raffaele Ragni
 */
public class SampleDAO {

  @Inject Jdbi jdbi;

  @Inject
  public SampleDAO() {
  }

  public String test() {
    return jdbi.withHandle(handle -> {
      return handle.createQuery("select 1 from dual")
        .mapTo(String.class)
        .findOnly();
    });
  }

}

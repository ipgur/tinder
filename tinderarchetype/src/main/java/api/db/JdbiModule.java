package api.db;

import dagger.Module;
import dagger.Provides;
import org.jdbi.v3.core.Jdbi;
import tinder.core.JDBILoader;

/**
 *
 * @author Raffaele Ragni
 */
@Module
public class JdbiModule {

  @Provides Jdbi jdbi() {
    return JDBILoader.load();
  }
  
}

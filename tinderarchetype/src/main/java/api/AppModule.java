package api;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import tinder.core.modules.ImmutableTinderConfiguration;
import tinder.core.modules.TinderConfiguration;
import tinder.core.modules.TinderModule;

@Module
public class AppModule extends TinderModule {

  public AppModule() {
    super(ImmutableTinderConfiguration.builder()
        // Add your configuration here...
        .sparkStaticFilesLocation("/docs")
        .build());
  }

  @Provides
  @Singleton
  public TinderConfiguration getConfiguration() {
    return configuration();
  }

  @Provides
  @Singleton
  public Jdbi getJdbi(TinderConfiguration configuration) {
    return jdbi(configuration);
  }

}

package api;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.timgroup.statsd.StatsDClient;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import tinder.core.auth.AuthenticationFilter;
import tinder.core.modules.ImmutableTinderConfiguration;
import tinder.core.modules.TinderConfiguration;
import tinder.core.modules.TinderModule;
import tinder.core.modules.metrics.StatsDHelper;

@Module
public class AppModule extends TinderModule {

  public AppModule() {
    super(ImmutableTinderConfiguration.builder()
        // Add your configuration here...
        .httpStaticFilesLocation("/docs")
        .build());

    // Add this before anything else, for obvious security reasons.
    // This constructor is the perfect place for it, but you can also move it to the app,
    // only be careful on the order of call.
    AuthenticationFilter.addJWTBasedFilter("/auth/*", jwtSecret());
  }

  @Provides
  @Singleton
  public TinderConfiguration getConfiguration() {
    return configuration();
  }

  @Provides
  @Singleton
  public HealthCheckRegistry getHealthCheckRegistry() {
    return healthCheckRegistry();
  }

  @Provides
  @Singleton
  public StatsDClient getStatsDClient() {
    return statsDClient();
  }

  @Provides
  @Singleton
  public StatsDHelper getStatsDHelper() {
    return new StatsDHelper(statsDClient());
  }

  @Provides
  @Singleton
  public Jdbi getJdbi(TinderConfiguration configuration) {
    return jdbi(configuration);
  }

  @Provides
  @Named("jwt_secret")
  public String jwtSecret() {
    return "big jwt secret passphrase used to sign jwt web tokens";
  }

}

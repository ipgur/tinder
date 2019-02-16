package api;

import api.db.JdbiModule;
import dagger.Component;

@Component(modules = {JdbiModule.class})
public interface AppComponent {
  App app();
}

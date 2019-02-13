package api.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tinder.patterns.polling.Poller;
import tinder.patterns.polling.ValueChangedProducer;

public class ConfigReloader {
  
  private static final Logger LOG = LoggerFactory.getLogger(ConfigReloader.class);
  
  private static final String CONFIG_PATH = "config.yaml";
  
  private final Poller<String> poller;
  
  @Inject
  public ConfigReloader() {
    poller = Poller
      .poller(
        ValueChangedProducer.map(this::checkFile),
        this::reloadConfig)
      .min(1).max(60);
    
    LOG.debug("Starting config reloader thread.");
    Thread thread = new Thread(poller);
    thread.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> thread.interrupt()));
  }
  
  public String checkFile() {
    try {
      LOG.debug("checking for config changes...");
      return Files.readString(Paths.get(CONFIG_PATH));
    } catch (IOException ex) {
      LOG.debug("no config file found.");
      return null;
    }
  }
  
  public void reloadConfig(String configYaml) {
    LOG.debug("Received new configuration to reload: {}", configYaml);
  }
  
}

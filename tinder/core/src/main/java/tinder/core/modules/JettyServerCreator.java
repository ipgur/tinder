/*
 * Copyright 2019 Raffaele Ragni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tinder.core.modules;

import java.util.function.Supplier;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Raffaele Ragni
 */
public class JettyServerCreator implements Supplier<Server> {

  private static final String JETTY_PREFIX = "Jetty :: ";

  private static final Logger LOG = LoggerFactory.getLogger(JettyServerCreator.class);

  private final TinderConfiguration configuration;
  private final Server server;

  public JettyServerCreator(TinderConfiguration configuration) {
    this.configuration = configuration;

    server = new Server();

    if (!configuration.httpSSLOnly()) {
      LOG.info(JETTY_PREFIX+"Adding UNsecured connection on {}", configuration.httpPort());
      ServerConnector connector = new ServerConnector(server);
      connector.setPort(configuration.httpPort());
      server.addConnector(connector);
    }

    // SSL Context Factory for HTTPS and HTTP/2
    SslContextFactory sslContextFactory = new SslContextFactory();
    configuration.httpSSLKeystorePath().ifPresent(kfile -> {
      configuration.httpSSLKeystorePassword().ifPresent(kpass -> {
        sslContextFactory.setKeyStorePath(kfile);
        sslContextFactory.setKeyStorePassword(kpass);
      });
    });
    sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
    sslContextFactory.setProvider("Conscrypt");

    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSendServerVersion(false);
    httpConfig.setSecureScheme("https");
    httpConfig.setSecurePort(configuration.httpSSLPort());
    httpConfig.addCustomizer(new SecureRequestCustomizer());

    // HTTP/2 Connection Factory
    HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
    ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
    alpn.setDefaultProtocol("h2");

    // SSL Connection Factory
    SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

    // HTTPS port + HTTP 2
    LOG.info(JETTY_PREFIX+"Adding secured connection on {}", configuration.httpSSLPort());
    ServerConnector http2Connector = new ServerConnector(server, ssl, alpn, h2, new HttpConnectionFactory(httpConfig));
    http2Connector.setPort(configuration.httpSSLPort());
    server.addConnector(http2Connector);

    GzipHandler gzipHandler = new GzipHandler();
    gzipHandler.setIncludedPaths("/*");
    server.setHandler(gzipHandler);

    LOG.info(JETTY_PREFIX+"Server created");
  }

  @Override
  public Server get() {
    return server;
  }

}

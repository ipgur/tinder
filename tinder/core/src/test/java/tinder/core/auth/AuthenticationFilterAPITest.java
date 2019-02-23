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
package tinder.core.auth;

import static java.util.Optional.empty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import spark.HaltException;
import spark.Request;
import spark.Response;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationFilterAPITest {

  @Test
  public void testAPI() {

    Request req = mock(Request.class);
    Response resp = mock(Response.class);

    when(req.headers(any())).thenReturn("Bearer XXX");
    when(req.uri()).thenReturn("/someendpoint");

    AuthenticationService service = mock(AuthenticationService.class);

    when(service.checkUrl(any())).thenReturn("email");
    AuthenticationFilter.authenticateAPIFilter(service, empty(), req, resp);

    when(service.checkUrl(any())).thenReturn(null);
    Assertions.assertThrows(HaltException.class, () -> {
      AuthenticationFilter.authenticateAPIFilter(service, empty(), req, resp);
    });

    // Tes skippping of login endpoint
    when(req.uri()).thenReturn("/login");
    AuthenticationFilter.authenticateAPIFilter(service, empty(), req, resp);
  }

}

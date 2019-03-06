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

import io.javalin.Context;
import io.javalin.HttpResponseException;
import static java.util.Optional.empty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationFilterAPITest {

  @Test
  public void testAPI() {

    Context ctx = mock(Context.class);

    when(ctx.header(any())).thenReturn("Bearer XXX");
    when(ctx.path()).thenReturn("/someendpoint");

    AuthenticationService service = mock(AuthenticationService.class);

    when(service.checkUrl(any())).thenReturn("email");
    AuthenticationFilter.authenticateAPIFilter(service, empty(), ctx);

    when(service.checkUrl(any())).thenReturn(null);
    Assertions.assertThrows(HttpResponseException.class, () -> {
      AuthenticationFilter.authenticateAPIFilter(service, empty(), ctx);
    });

    // Tes skippping of login endpoint
    when(ctx.path()).thenReturn("/login");
    AuthenticationFilter.authenticateAPIFilter(service, empty(), ctx);
  }

}

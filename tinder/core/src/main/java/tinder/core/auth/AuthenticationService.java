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

import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * This authentication service is working as the same as for /checkurl but remotely towards a secondary API.
 * This means it will be used in the filter to contact the authentication API, if you build the satellite model.
 * In the satellite model one API is using the AuthenticationResource set up and the api using the AuthenticationFilter
 * contacts that API on the back for each token used.
 * @author Raffaele Ragni
 */
public interface AuthenticationService {
  @POST("/checkurl")
  String checkUrl(@Header("Authorization") String authorization);
}

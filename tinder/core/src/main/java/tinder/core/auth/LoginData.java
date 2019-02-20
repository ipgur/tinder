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

import org.immutables.value.Value.Immutable;

/**
 * Login data, also used for registration.
 * @author Raffaele Ragni
 */
@Immutable
public interface LoginData {

  /**
   * Email is also the username / user identifier.
   * @return the email
   */
  String email();

  /**
   * Password is the secret.
   * @return return the secret password
   */
  String password();
}
/*
 * Copyright 2018 Raffaele Ragni.
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
package tinder.core;

/**
 * Base and interface marker for REST Resources.
 * Methods can be overridden so that the user can add implementation before the generated class executes its own.
 * @author Raffaele Ragni
 */
public interface ResourceEvents {

  /**
   * Initialization method called at resource creation.
   */
  default void init() {}

}

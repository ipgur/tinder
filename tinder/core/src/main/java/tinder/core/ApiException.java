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
package tinder.core;

/**
 *
 * @author Raffaele Ragni
 */
public class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  private final ApiMessage apiMessage;
  
  public ApiException() {
    super();
    apiMessage = ImmutableApiMessage.builder().build();
  }

  public ApiException(ApiMessage apiMessage) {
    this.apiMessage = apiMessage;
  }

  public ApiException(String message) {
    super(message);
    apiMessage = ImmutableApiMessage.builder().message(message).build();
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
    apiMessage = ImmutableApiMessage.builder().message(message).build();
  }
  
  public ApiMessage getApiMessage() {
    return apiMessage;
  }

}

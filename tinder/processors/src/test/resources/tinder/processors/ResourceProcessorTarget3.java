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
package tinder.processors;

import io.javalin.Context;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import tinder.core.Resource;
import tinder.core.ResourceEvents;

/**
 *
 * @author Raffaele Ragni
 */
@Resource
public class ResourceProcessorTarget3 implements ResourceEvents {

  private String justAString;

  @GET
  @Path("get2")
  public void test2(String param1, Context ctx) {
  }

  @GET
  @Path("get3")
  public String test3(Context ctx) {
    return "";
  }

}

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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import tinder.core.Resource;

/**
 *
 * @author Raffaele Ragni
 */
@Resource
@Path("/")
public class ResourceProcessorTarget1 {

  @GET
  @Path("/get/{id}")
  @Produces("application/json")
  public String test(
      @PathParam("id") Long id,
      @QueryParam("limit") Long limit,
      @HeaderParam("Content-Length") Long contentLength) {
    return "";
  }

  @POST
  @Path("/echo/")
  @Consumes("text/plain")
  @Produces("text/plain")
  public String echo(String input) {
    return input;
  }

  @PATCH
  @Path("/raw2")
  public void raw2(Context ctx) {

  }

  @PUT
  @Path("/raw")
  public void raw(Context ctx) {

  }

  @DELETE
  @Path("/delete/{id}")
  public void raw(@PathParam("id") Long id) {

  }
}
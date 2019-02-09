package api.resources;

import api.model.Bean;
import api.model.ImmutableBean;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import spark.Request;
import spark.Response;
import tinder.core.Resource;
import tinder.core.ResourceEvents;

@Resource
@Path("/")
public class Example implements ResourceEvents {

  @GET
  @Path("/test/{id}")
  @Produces("application/json")
  public Bean test(
      @PathParam("id") Long id,
      @QueryParam("limit") Long limit,
      @HeaderParam("Content-Length") Long contentLength) {
    return ImmutableBean.builder()
        .id(id)
        .limit(limit)
        .length(contentLength)
        .build();
  }

  @POST
  @Path("/echo/")
  @Consumes("text/plain")
  @Produces("text/plain")
  public String echo(String input) {
    return input;
  }

  @PUT
  @Path("/raw")
  public void raw(Request req, Response resp) {

  }

}

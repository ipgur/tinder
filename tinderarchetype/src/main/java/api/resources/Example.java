package api.resources;

import api.model.ImmutableBean;
import api.services.ServiceExample;
import io.swagger.annotations.ApiOperation;
import javax.inject.Inject;
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

  @Inject ServiceExample serviceExample;

  @Inject
  public Example() {
    ResourceExample.bind(this);
  }

  @GET
  @Path("/test/{id}")
  @Produces("application/json")
  @ApiOperation(value = "Returns a Bean",
    notes = "Just a normal endpoint",
    response = ImmutableBean.class)
  public ImmutableBean test(
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

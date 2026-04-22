package com.extraction.client;

import com.extraction.dto.WazeResponseDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "waze-api")
@Path("/live-map/api/georss")
public interface WazeRestClient {

  @GET
  WazeResponseDto getAlerts(
      @QueryParam("top") double top,
      @QueryParam("bottom") double bottom,
      @QueryParam("left") double left,
      @QueryParam("right") double right,
      @QueryParam("env") String env,
      @QueryParam("types") String types);
}

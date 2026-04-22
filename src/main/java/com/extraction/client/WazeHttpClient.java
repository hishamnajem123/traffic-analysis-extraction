package com.extraction.client;

import com.extraction.dto.WazeAlertDto;
import com.extraction.dto.WazeResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WazeHttpClient implements WazeClient {

  private static final Logger LOG = Logger.getLogger(WazeHttpClient.class);

  @Inject @RestClient WazeRestClient restClient;

  @ConfigProperty(name = "waze.bbox.top")
  double top;

  @ConfigProperty(name = "waze.bbox.bottom")
  double bottom;

  @ConfigProperty(name = "waze.bbox.left")
  double left;

  @ConfigProperty(name = "waze.bbox.right")
  double right;

  @ConfigProperty(name = "waze.env")
  String env;

  @ConfigProperty(name = "waze.types")
  String types;

  @ConfigProperty(name = "waze.jitter")
  double jitter;

  @Override
  public List<WazeAlertDto> fetchAlerts() {
    Bbox bbox = jitteredBbox();

    try {
      WazeResponseDto response =
          restClient.getAlerts(bbox.top(), bbox.bottom(), bbox.left(), bbox.right(), env, types);

      List<WazeAlertDto> alerts =
          response != null && response.alerts() != null
              ? response.alerts()
              : Collections.emptyList();

      LOG.infof("Fetched %d alerts from Waze", alerts.size());

      return alerts;

    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch alerts from Waze", e);
    }
  }

  private Bbox jitteredBbox() {
    double height = top - bottom;
    double width = right - left;

    double centerLat = (top + bottom) / 2.0;
    double centerLon = (left + right) / 2.0;

    double jitteredCenterLat = centerLat + randomOffset();
    double jitteredCenterLon = centerLon + randomOffset();

    return new Bbox(
        jitteredCenterLat + height / 2.0,
        jitteredCenterLat - height / 2.0,
        jitteredCenterLon - width / 2.0,
        jitteredCenterLon + width / 2.0);
  }

  private double randomOffset() {
    return ThreadLocalRandom.current().nextDouble(-jitter, jitter);
  }

  private record Bbox(double top, double bottom, double left, double right) {}
}

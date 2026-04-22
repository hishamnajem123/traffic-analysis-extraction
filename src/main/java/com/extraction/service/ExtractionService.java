package com.extraction.service;

import com.extraction.client.WazeClient;
import com.extraction.dto.WazeAlertDto;
import com.extraction.mapper.WazeAlertMapper;
import com.extraction.model.ExtractionResult;
import com.extraction.model.SnapshotDiff;
import com.extraction.model.TrafficAlert;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ExtractionService {

  private static final Logger LOG = Logger.getLogger(ExtractionService.class);

  @Inject WazeClient wazeClient;

  @Inject WazeAlertMapper wazeAlertMapper;

  @Inject SnapshotDiffService snapshotDiffService;

  @Inject AlertPublisher alertPublisher;

  private Map<String, TrafficAlert> previousSnapshot = new LinkedHashMap<>();

  public ExtractionResult fetchAndProcess() {
    List<WazeAlertDto> rawAlerts = wazeClient.fetchAlerts();

    Map<String, TrafficAlert> currentSnapshot = normalizeAlerts(rawAlerts);

    SnapshotDiff snapshotDiff = snapshotDiffService.diff(previousSnapshot, currentSnapshot);

    if (!snapshotDiff.newAlerts().isEmpty()) {
      alertPublisher.publishNewAlerts(snapshotDiff.newAlerts());
    }

    previousSnapshot = currentSnapshot;

    ExtractionResult result =
        ExtractionResult.builder()
            .rawAlertCount(rawAlerts.size())
            .validAlertCount(currentSnapshot.size())
            .newAlertCount(snapshotDiff.newAlerts().size())
            .existingAlertCount(snapshotDiff.existingAlerts().size())
            .removedAlertCount(snapshotDiff.removedAlerts().size())
            .build();

    LOG.infof(
        "Extraction complete: raw=%d valid=%d new=%d existing=%d removed=%d",
        result.rawAlertCount(),
        result.validAlertCount(),
        result.newAlertCount(),
        result.existingAlertCount(),
        result.removedAlertCount());

    return result;
  }

  private Map<String, TrafficAlert> normalizeAlerts(List<WazeAlertDto> rawAlerts) {
    Map<String, TrafficAlert> normalizedAlerts = new LinkedHashMap<>();

    for (WazeAlertDto rawAlert : rawAlerts) {
      if (rawAlert == null || rawAlert.uuid() == null || rawAlert.uuid().isBlank()) {
        continue;
      }

      try {
        TrafficAlert trafficAlert = wazeAlertMapper.toTrafficAlert(rawAlert);

        if (!isValid(trafficAlert)) {
          continue;
        }

        normalizedAlerts.put(trafficAlert.externalId(), trafficAlert);

      } catch (Exception e) {
        LOG.warnf(e, "Failed to normalize Waze alert with raw id [%s]", rawAlert.id());
      }
    }

    return normalizedAlerts;
  }

  private boolean isValid(TrafficAlert trafficAlert) {
    return trafficAlert != null
        && trafficAlert.externalId() != null
        && !trafficAlert.externalId().isBlank()
        && trafficAlert.type() != null
        && trafficAlert.latitude() != null
        && trafficAlert.longitude() != null;
  }
}

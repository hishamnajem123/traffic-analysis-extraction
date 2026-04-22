package com.extraction.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record SnapshotDiff(
    List<TrafficAlert> newAlerts,
    List<TrafficAlert> existingAlerts,
    List<TrafficAlert> removedAlerts) {

  public static SnapshotDiff between(
      Map<String, TrafficAlert> previousSnapshot, Map<String, TrafficAlert> currentSnapshot) {
    List<TrafficAlert> newAlerts = new ArrayList<>();
    List<TrafficAlert> existingAlerts = new ArrayList<>();
    List<TrafficAlert> removedAlerts = new ArrayList<>();

    for (Map.Entry<String, TrafficAlert> entry : currentSnapshot.entrySet()) {
      String alertId = entry.getKey();
      TrafficAlert currentAlert = entry.getValue();

      if (previousSnapshot.containsKey(alertId)) {
        existingAlerts.add(currentAlert);
      } else {
        newAlerts.add(currentAlert);
      }
    }

    for (Map.Entry<String, TrafficAlert> entry : previousSnapshot.entrySet()) {
      String alertId = entry.getKey();
      TrafficAlert previousAlert = entry.getValue();

      if (!currentSnapshot.containsKey(alertId)) {
        removedAlerts.add(previousAlert);
      }
    }

    return SnapshotDiff.builder()
        .newAlerts(newAlerts)
        .existingAlerts(existingAlerts)
        .removedAlerts(removedAlerts)
        .build();
  }
}

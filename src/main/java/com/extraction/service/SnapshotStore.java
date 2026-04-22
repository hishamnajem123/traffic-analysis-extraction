package com.extraction.service;

import com.extraction.model.SnapshotDiff;
import com.extraction.model.TrafficAlert;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class SnapshotStore {

  private Map<String, TrafficAlert> snapshot = Map.of();

  SnapshotDiff diffAndReplace(Map<String, TrafficAlert> nextSnapshot) {
    SnapshotDiff snapshotDiff = SnapshotDiff.between(snapshot, nextSnapshot);
    snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(nextSnapshot));
    return snapshotDiff;
  }

  void reset() {
    snapshot = Map.of();
  }
}

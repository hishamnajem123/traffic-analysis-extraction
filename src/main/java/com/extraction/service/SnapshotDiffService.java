package com.extraction.service;

import com.extraction.model.SnapshotDiff;
import com.extraction.model.TrafficAlert;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class SnapshotDiffService {

  public SnapshotDiff diff(
      Map<String, TrafficAlert> previousSnapshot, Map<String, TrafficAlert> currentSnapshot) {
    return null;
  }
}

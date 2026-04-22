package com.extraction.model;

import java.util.List;
import lombok.Builder;

@Builder
public record SnapshotDiff(
    List<TrafficAlert> newAlerts,
    List<TrafficAlert> existingAlerts,
    List<TrafficAlert> removedAlerts) {}

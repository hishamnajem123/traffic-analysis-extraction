package com.extraction.model;

import lombok.Builder;

@Builder
public record TrafficAlert(
    String externalId,
    String rawId,
    AlertType type,
    String subtype,
    String street,
    String city,
    Double latitude,
    Double longitude,
    Long publishedAtMillis,
    Integer reliability,
    Integer confidence,
    String description,
    Boolean inScale) {
  public TrafficAlert {
    if (externalId == null || externalId.isBlank()) {
      throw new IllegalArgumentException("externalId is required");
    }
    if (type == null) {
      throw new IllegalArgumentException("type is required");
    }
    if (latitude == null || longitude == null) {
      throw new IllegalArgumentException("latitude/longitude are required");
    }
  }
}

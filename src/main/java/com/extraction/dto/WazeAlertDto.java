package com.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record WazeAlertDto(
    String uuid,
    String id,
    String type,
    String subtype,
    String street,
    String city,
    Long pubMillis,
    Integer reliability,
    Integer confidence,
    String reportDescription,
    @JsonProperty("inscale") Boolean inScale,
    WazeLocationDto location) {}

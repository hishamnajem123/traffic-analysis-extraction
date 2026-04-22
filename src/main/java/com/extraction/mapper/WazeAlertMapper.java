package com.extraction.mapper;

import com.extraction.dto.WazeAlertDto;
import com.extraction.model.AlertType;
import com.extraction.model.TrafficAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface WazeAlertMapper {

  @Mapping(target = "externalId", source = "uuid")
  @Mapping(target = "rawId", source = "id")
  @Mapping(target = "latitude", source = "location.y")
  @Mapping(target = "longitude", source = "location.x")
  @Mapping(target = "publishedAtMillis", source = "pubMillis")
  @Mapping(target = "description", source = "reportDescription")
  @Mapping(target = "type", expression = "java(mapType(dto.type()))")
  TrafficAlert toTrafficAlert(WazeAlertDto dto);

  // ---- custom mapping ----

  default AlertType mapType(String type) {
    if (type == null) return null;

    try {
      return AlertType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      return AlertType.UNKNOWN;
    }
  }
}

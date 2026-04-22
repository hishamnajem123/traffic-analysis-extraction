package com.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WazeResponseDto(List<WazeAlertDto> alerts) {}

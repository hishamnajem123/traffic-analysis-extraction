package com.extraction.client;

import com.extraction.dto.WazeAlertDto;
import java.util.List;

public interface WazeClient {
  List<WazeAlertDto> fetchAlerts();
}

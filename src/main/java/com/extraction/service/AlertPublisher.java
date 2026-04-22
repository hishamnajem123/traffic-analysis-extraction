package com.extraction.service;

import com.extraction.model.TrafficAlert;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class AlertPublisher {
  void publishNewAlerts(List<TrafficAlert> alerts) {}
}

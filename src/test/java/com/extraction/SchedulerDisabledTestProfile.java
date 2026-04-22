package com.extraction;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class SchedulerDisabledTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("quarkus.scheduler.enabled", "false");
  }
}

package com.extraction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.extraction.SchedulerDisabledTestProfile;
import com.extraction.client.WazeClient;
import com.extraction.dto.WazeAlertDto;
import com.extraction.dto.WazeLocationDto;
import com.extraction.model.ExtractionResult;
import com.extraction.model.TrafficAlert;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(SchedulerDisabledTestProfile.class)
class ExtractionServiceTest {

  @Inject ExtractionService extractionService;

  @Inject SnapshotStore snapshotStore;

  @InjectMock WazeClient wazeClient;

  @InjectMock AlertPublisher alertPublisher;

  @BeforeEach
  void resetState() {
    snapshotStore.reset();
  }

  @Test
  void fetchAndProcessDiffsAgainstPreviousSnapshot() {
    WazeAlertDto original = alert("alert-1");
    WazeAlertDto removed = alert("alert-2");
    WazeAlertDto added = alert("alert-3");

    when(wazeClient.fetchAlerts())
        .thenReturn(List.of(original, removed))
        .thenReturn(List.of(original, added));

    ExtractionResult firstResult = extractionService.fetchAndProcess();

    assertEquals(2, firstResult.rawAlertCount());
    assertEquals(2, firstResult.validAlertCount());
    assertEquals(2, firstResult.newAlertCount());
    assertEquals(0, firstResult.existingAlertCount());
    assertEquals(0, firstResult.removedAlertCount());

    verify(alertPublisher)
        .publishNewAlerts(
            argThat(
                (List<TrafficAlert> alerts) ->
                    externalIds(alerts).equals(List.of("alert-1", "alert-2"))));

    reset(alertPublisher);

    ExtractionResult secondResult = extractionService.fetchAndProcess();

    assertEquals(2, secondResult.rawAlertCount());
    assertEquals(2, secondResult.validAlertCount());
    assertEquals(1, secondResult.newAlertCount());
    assertEquals(1, secondResult.existingAlertCount());
    assertEquals(1, secondResult.removedAlertCount());

    verify(alertPublisher)
        .publishNewAlerts(
            argThat(
                (List<TrafficAlert> alerts) -> externalIds(alerts).equals(List.of("alert-3"))));
  }

  private WazeAlertDto alert(String uuid) {
    return WazeAlertDto.builder()
        .uuid(uuid)
        .id("raw-" + uuid)
        .type("POLICE")
        .subtype("POLICE_HIDING")
        .street("Kerr Ave")
        .city("Ottawa")
        .pubMillis(1776888854000L)
        .reliability(6)
        .confidence(0)
        .reportDescription("")
        .inScale(false)
        .location(new WazeLocationDto(-75.752694, 45.376679))
        .build();
  }

  private List<String> externalIds(List<TrafficAlert> alerts) {
    return alerts.stream().map(TrafficAlert::externalId).toList();
  }
}

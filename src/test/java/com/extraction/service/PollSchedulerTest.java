package com.extraction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.extraction.client.WazeClient;
import com.extraction.dto.WazeAlertDto;
import com.extraction.dto.WazeResponseDto;
import com.extraction.model.SnapshotDiff;
import com.extraction.model.TrafficAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(PollSchedulerTest.Profile.class)
class PollSchedulerTest {

  private static final Path WAZE_SAMPLE =
      Path.of("src/test/java/com/extraction/resources/waze_georss_sample");

  @Inject PollScheduler pollScheduler;

  @Inject MeterRegistry meterRegistry;

  @Inject ObjectMapper objectMapper;

  @InjectMock WazeClient wazeClient;

  @InjectMock SnapshotDiffService snapshotDiffService;

  @InjectMock AlertPublisher alertPublisher;

  @Test
  void scheduledPollFetchesAlertsAndRecordsSuccess() throws IOException {
    List<WazeAlertDto> alerts = sampleAlerts();
    when(wazeClient.fetchAlerts()).thenReturn(alerts);
    when(snapshotDiffService.diff(anyMap(), anyMap()))
        .thenAnswer(
            invocation -> {
              Map<String, TrafficAlert> currentSnapshot = invocation.getArgument(1);
              return SnapshotDiff.builder()
                  .newAlerts(new ArrayList<>(currentSnapshot.values()))
                  .existingAlerts(List.of())
                  .removedAlerts(List.of())
                  .build();
            });

    double successBefore = counterValue("waze.poll.success.total");
    double failureBefore = counterValue("waze.poll.failure.total");

    pollScheduler.scheduledPoll();

    assertEquals(successBefore + 1.0, counterValue("waze.poll.success.total"));
    assertEquals(failureBefore, counterValue("waze.poll.failure.total"));
    verify(wazeClient).fetchAlerts();
    verify(snapshotDiffService).diff(anyMap(), anyMap());
    verify(alertPublisher).publishNewAlerts(anyList());
  }

  @Test
  void scheduledPollRecordsFailureWhenClientThrows() {
    when(wazeClient.fetchAlerts()).thenThrow(new RuntimeException("waze unavailable"));

    double successBefore = counterValue("waze.poll.success.total");
    double failureBefore = counterValue("waze.poll.failure.total");

    pollScheduler.scheduledPoll();

    assertEquals(successBefore, counterValue("waze.poll.success.total"));
    assertEquals(failureBefore + 1.0, counterValue("waze.poll.failure.total"));
    verify(wazeClient).fetchAlerts();
    verify(snapshotDiffService, never()).diff(anyMap(), anyMap());
    verify(alertPublisher, never()).publishNewAlerts(anyList());
  }

  @Test
  void scheduledPollSkipsWhenPreviousPollIsStillRunning() throws Exception {
    CountDownLatch fetchStarted = new CountDownLatch(1);
    CountDownLatch releaseFetch = new CountDownLatch(1);
    when(wazeClient.fetchAlerts())
        .thenAnswer(
            invocation -> {
              fetchStarted.countDown();
              releaseFetch.await(5, TimeUnit.SECONDS);
              return List.of();
            });
    when(snapshotDiffService.diff(anyMap(), anyMap()))
        .thenReturn(
            SnapshotDiff.builder()
                .newAlerts(List.of())
                .existingAlerts(List.of())
                .removedAlerts(List.of())
                .build());

    double skippedBefore = counterValue("waze.poll.skipped.total");
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    try {
      Future<?> firstPoll = executorService.submit(() -> pollScheduler.scheduledPoll());
      fetchStarted.await(5, TimeUnit.SECONDS);

      pollScheduler.scheduledPoll();

      releaseFetch.countDown();
      firstPoll.get(5, TimeUnit.SECONDS);
    } finally {
      releaseFetch.countDown();
      executorService.shutdownNow();
    }

    assertEquals(skippedBefore + 1.0, counterValue("waze.poll.skipped.total"));
    verify(wazeClient, times(1)).fetchAlerts();
  }

  private List<WazeAlertDto> sampleAlerts() throws IOException {
    WazeResponseDto response = objectMapper.readValue(WAZE_SAMPLE.toFile(), WazeResponseDto.class);
    return response.alerts();
  }

  private double counterValue(String name) {
    var counter = meterRegistry.find(name).counter();
    return counter == null ? 0.0 : counter.count();
  }

  public static class Profile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("quarkus.scheduler.enabled", "false");
    }
  }
}

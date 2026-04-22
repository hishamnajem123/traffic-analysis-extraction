package com.extraction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.extraction.SchedulerDisabledTestProfile;
import com.extraction.client.WazeClient;
import com.extraction.dto.WazeAlertDto;
import com.extraction.dto.WazeResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(SchedulerDisabledTestProfile.class)
class PollSchedulerTest {

  private static final Path WAZE_SAMPLE =
      Path.of("src/test/java/com/extraction/resources/waze_georss_sample");

  @Inject PollScheduler pollScheduler;

  @Inject MeterRegistry meterRegistry;

  @Inject ObjectMapper objectMapper;

  @Inject SnapshotStore snapshotStore;

  @InjectMock WazeClient wazeClient;

  @InjectMock AlertPublisher alertPublisher;

  @BeforeEach
  void resetState() {
    snapshotStore.reset();
  }

  @Test
  void scheduledPollFetchesAlertsAndRecordsSuccess() throws IOException {
    List<WazeAlertDto> alerts = sampleAlerts();
    when(wazeClient.fetchAlerts()).thenReturn(alerts);

    double successBefore = counterValue("waze.poll.success.total");
    double failureBefore = counterValue("waze.poll.failure.total");

    pollScheduler.scheduledPoll();

    assertEquals(successBefore + 1.0, counterValue("waze.poll.success.total"));
    assertEquals(failureBefore, counterValue("waze.poll.failure.total"));
    verify(wazeClient).fetchAlerts();
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
}

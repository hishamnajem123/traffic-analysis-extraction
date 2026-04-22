package com.extraction.service;

import com.extraction.model.ExtractionResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PollScheduler {

  private static final Logger LOG = Logger.getLogger(PollScheduler.class);

  private final AtomicBoolean polling = new AtomicBoolean(false);

  @Inject ExtractionService extractionService;

  @Inject MeterRegistry meterRegistry;

  private Timer pollTimer;
  private Counter pollSuccessCounter;
  private Counter pollFailureCounter;
  private Counter pollSkippedCounter;

  @PostConstruct
  void init() {
    pollTimer = Timer.builder("waze.poll.duration").register(meterRegistry);

    pollSuccessCounter = meterRegistry.counter("waze.poll.success.total");
    pollFailureCounter = meterRegistry.counter("waze.poll.failure.total");
    pollSkippedCounter = meterRegistry.counter("waze.poll.skipped.total");
  }

  @Scheduled(identity = "waze-poller", every = "10s")
  void scheduledPoll() {
    if (!polling.compareAndSet(false, true)) {
      pollSkippedCounter.increment();
      LOG.warn("Previous poll is still running, skipping this cycle");
      return;
    }

    long startNanos = System.nanoTime();

    try {
      LOG.debug("Starting Waze poll");

      ExtractionResult result = extractionService.fetchAndProcess();

      long durationNanos = System.nanoTime() - startNanos;
      pollTimer.record(durationNanos, TimeUnit.NANOSECONDS);
      pollSuccessCounter.increment();

      long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
      LOG.infof(
          "Waze poll completed successfully in %d ms (raw=%d, valid=%d, new=%d, removed=%d)",
          durationMs,
          result.rawAlertCount(),
          result.validAlertCount(),
          result.newAlertCount(),
          result.removedAlertCount());
    } catch (Exception e) {
      long durationNanos = System.nanoTime() - startNanos;
      pollTimer.record(durationNanos, TimeUnit.NANOSECONDS);
      pollFailureCounter.increment();

      long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
      LOG.errorf(e, "Waze poll failed after %d ms", durationMs);

    } finally {
      polling.set(false);
    }
  }
}

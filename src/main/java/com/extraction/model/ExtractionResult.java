package com.extraction.model;

import lombok.Builder;

@Builder
public record ExtractionResult(
    int rawAlertCount,
    int validAlertCount,
    int newAlertCount,
    int existingAlertCount,
    int removedAlertCount) {}

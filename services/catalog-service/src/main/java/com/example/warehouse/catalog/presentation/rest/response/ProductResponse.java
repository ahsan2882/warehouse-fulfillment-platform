package com.example.warehouse.catalog.presentation.rest.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for product resources (basic version for now).
 */
public record ProductResponse(

    String id,

    String sku,

    String name,

    String description,

    String unitOfMeasure,

    boolean isActive,

    LocalDateTime createdAt

) {}

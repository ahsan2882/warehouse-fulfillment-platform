package com.example.warehouse.catalog.presentation.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating an existing product (partial update).
 */
public record UpdateProductRequest(

    @NotBlank(message = "Name is required")
    String name,

    String description,

    String unitOfMeasure,

    Boolean isActive
) {}

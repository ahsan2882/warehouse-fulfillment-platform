package com.example.warehouse.catalog.presentation.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new product.
 */
public record CreateProductRequest(

    @NotBlank(message = "SKU is required")
    String sku,

    @NotBlank(message = "Name is required")
    String name,

    String description,

    @NotNull(message = "Unit of measure is required")
    String unitOfMeasure
) {}

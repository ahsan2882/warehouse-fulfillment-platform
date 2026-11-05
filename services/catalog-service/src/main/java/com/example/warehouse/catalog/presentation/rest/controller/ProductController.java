package com.example.warehouse.catalog.presentation.rest.controller;

import com.example.warehouse.catalog.domain.Product;
import com.example.warehouse.catalog.domain.repository.ProductRepository;
import com.example.warehouse.catalog.presentation.rest.request.CreateProductRequest;
import com.example.warehouse.catalog.presentation.rest.request.UpdateProductRequest;
import com.example.warehouse.catalog.presentation.rest.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Products", description = "Product management endpoints")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "List all products", description = "Returns paginated list of all products")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = repository.findAll();
        return ResponseEntity.ok(products.stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns a single product by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID")
            @PathVariable String id) {

        return repository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Returns a product by its unique SKU")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "Product SKU")
            @PathVariable String sku) {

        return repository.findBySkuAndIsActiveTrue(sku)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new product", description = "Creates a new product in the catalog")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Bad request - validation error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid
            @RequestBody CreateProductRequest request) {

        Product product = new Product(
                request.sku(),
                request.name(),
                Product.UnitOfMeasure.valueOf(request.unitOfMeasure())
        );

        Product saved = repository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update product", description = "Partially updates an existing product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID")
            @PathVariable String id,

            @Valid
            @RequestBody UpdateProductRequest request) {

        Product existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (request.name() != null) {
            existing.setName(request.name());
        }
        if (request.description() != null) {
            existing.setDescription(request.description());
        }
        if (request.unitOfMeasure() != null) {
            existing.setUnitOfMeasure(Product.UnitOfMeasure.valueOf(request.unitOfMeasure()));
        }
        if (request.isActive() != null) {
            existing.setActive(request.isActive());
        }

        Product updated = repository.save(existing);
        return ResponseEntity.ok(toResponse(updated));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getUnitOfMeasure().name(),
                product.isActive(),
                product.getCreatedAt()
        );
    }
}

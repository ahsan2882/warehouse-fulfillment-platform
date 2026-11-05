package com.example.warehouse.catalog.presentation.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.warehouse.catalog.domain.Product;
import com.example.warehouse.catalog.domain.repository.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class CatalogServiceIntegrationTests {

    @Autowired
    private ProductRepository repository;

    @DynamicPropertySource
    static void configureDb(DynamicPropertyRegistry registry) {
        // Create a unique database for this test class to avoid cross-contamination
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb-" + System.nanoTime());
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void should_save_product_with_all_fields() {
        // Given
        String sku = "INTEG-001-" + System.nanoTime(); // Unique per test instance
        Product product = new Product(sku, "Integration Test Product", Product.UnitOfMeasure.PCS);

        // When
        Product saved = repository.save(product);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertEquals(sku, saved.getSku());
        assertTrue(repository.existsBySku(sku));
    }

    @Test
    void should_find_all_active_products() {
        // Given - setup with unique SKUs to avoid duplicate key violations
        String sku1 = "P1-" + System.nanoTime();
        String sku2 = "P2-" + System.nanoTime();
        Product p1 = new Product(sku1, "Active Product 1", Product.UnitOfMeasure.PCS);
        Product p2 = new Product(sku2, "Active Product 2", Product.UnitOfMeasure.KG);

        repository.save(p1);
        repository.save(p2);

        // When
        List<Product> activeProducts = repository.findByIsActiveTrue();

        // Then
        assertEquals(2, activeProducts.size());
    }

    @Test
    void should_find_product_by_sku_when_active() {
        // Given - unique SKU for this test
        String sku = "SKU-001-" + System.nanoTime();
        Product product = new Product(sku, "Active Product", Product.UnitOfMeasure.PCS);
        product.setActive(true);
        repository.save(product);

        // When
        var found = repository.findBySkuAndIsActiveTrue(sku);

        // Then
        assertTrue(found.isPresent());
        assertEquals(sku, found.get().getSku());
    }

    @Test
    void should_not_find_inactive_product_by_sku_with_active_filter() {
        // Given - unique SKU for this test
        String sku = "SKU-002-" + System.nanoTime();
        Product product = new Product(sku, "Inactive Product", Product.UnitOfMeasure.PCS);
        product.setActive(false);
        repository.save(product);

        // When
        var found = repository.findBySkuAndIsActiveTrue(sku);

        // Then
        assertFalse(found.isPresent());
    }
}

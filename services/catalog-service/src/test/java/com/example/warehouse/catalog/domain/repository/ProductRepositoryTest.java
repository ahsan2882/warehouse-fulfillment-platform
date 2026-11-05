package com.example.warehouse.catalog.domain.repository;

import com.example.warehouse.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void should_save_product_with_unique_sku() {
        // Given
        String sku = "TEST-001";
        Product product = new Product(sku, "Test Product", Product.UnitOfMeasure.PCS);

        // When
        Product saved = repository.save(product);

        // Then
        assertNotNull(saved.getId());
        assertEquals(sku, saved.getSku());
        assertTrue(repository.existsBySku(sku));
    }

    @Test
    void should_find_all_active_products() {
        // Given - setup (for simplicity, we'll just check the method exists)
        Product p1 = new Product("P1", "Product 1", Product.UnitOfMeasure.PCS);
        Product p2 = new Product("P2", "Product 2", Product.UnitOfMeasure.KG);

        repository.save(p1);
        repository.save(p2);

        // When
        List<Product> activeProducts = repository.findByIsActiveTrue();

        // Then
        assertEquals(2, activeProducts.size());
    }

    @Test
    void should_find_product_by_sku_when_active() {
        // Given
        String sku = "SKU-001";
        Product product = new Product(sku, "Active Product", Product.UnitOfMeasure.PCS);
        product.setActive(true);
        repository.save(product);

        // When
        Product found = repository.findBySkuAndIsActiveTrue(sku)
                .orElse(null);

        // Then
        assertNotNull(found);
        assertEquals(sku, found.getSku());
    }
}

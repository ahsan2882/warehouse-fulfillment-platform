package com.example.warehouse.catalog.domain.repository;

import com.example.warehouse.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Check if a product with the given SKU exists.
     */
    boolean existsBySku(String sku);

    /**
     * Find all active products.
     */
    List<Product> findByIsActiveTrue();

    /**
     * Find an active product by SKU.
     */
    Optional<Product> findBySkuAndIsActiveTrue(String sku);
}

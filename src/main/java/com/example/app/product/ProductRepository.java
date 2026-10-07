package com.example.app.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Check and decrement in ONE statement: the database locks the row, so two orders can never both see
     * "enough stock" and oversell. Returns 1 if reserved, 0 if the product is missing or has too little stock.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity, p.version = p.version + 1 "
            + "where p.id = :id and p.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :quantity, p.version = p.version + 1 where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}

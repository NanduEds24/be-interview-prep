package com.example.app.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByCustomerEmailAndIdempotencyKey(String customerEmail, String idempotencyKey);

    Optional<CustomerOrder> findByIdAndCustomerEmail(Long id, String customerEmail);

    /**
     * PLACED -> CANCELLED in one statement. Returns 0 if it was already cancelled, so two cancels racing
     * each other can't both return the stock.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CustomerOrder o set o.status = com.example.app.order.OrderStatus.CANCELLED "
            + "where o.id = :id and o.status = com.example.app.order.OrderStatus.PLACED")
    int markCancelled(@Param("id") Long id);
}

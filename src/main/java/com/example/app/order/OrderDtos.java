package com.example.app.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public record PlaceOrderRequest(
            @NotEmpty(message = "items must contain at least one item")
            @Size(max = 50, message = "an order can have at most 50 items")
            List<@Valid @NotNull(message = "item is required") ItemRequest> items) {}

    public record ItemRequest(
            @NotNull(message = "productId is required") Long productId,
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be at least 1")
            @Max(value = 1000, message = "quantity must be at most 1000") Integer quantity) {}

    public record ItemResponse(Long productId, int quantity) {}

    public record OrderResponse(Long id, OrderStatus status, List<ItemResponse> items, Instant createdAt) {

        static OrderResponse from(CustomerOrder order) {
            List<ItemResponse> items = order.getItems().stream()
                    .map(i -> new ItemResponse(i.getProductId(), i.getQuantity())).toList();
            return new OrderResponse(order.getId(), order.getStatus(), items, order.getCreatedAt());
        }
    }

    /** created = false means this was a retry and the existing order is returned. */
    public record PlaceOrderResult(OrderResponse order, boolean created) {}
}

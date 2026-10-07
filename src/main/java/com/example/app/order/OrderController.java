package com.example.app.order;

import com.example.app.order.OrderDtos.OrderResponse;
import com.example.app.order.OrderDtos.PlaceOrderRequest;
import com.example.app.order.OrderDtos.PlaceOrderResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** 201 for a new order; 200 with the same order when the Idempotency-Key was already used (a retry). */
    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication authentication) {
        PlaceOrderResult result = service.place(authentication.getName(), idempotencyKey, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.order());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return service.get(authentication.getName(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
        return service.cancel(authentication.getName(), id);
    }
}

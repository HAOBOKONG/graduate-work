package com.example.market.controller;
import com.example.market.dto.CreateOrderRequest;
import com.example.market.service.AuthService;
import com.example.market.service.OrderService;
import com.example.market.vo.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final AuthService auth;
    public OrderController(OrderService orders,AuthService auth) { this.orders=orders; this.auth=auth; }
    private Long userId(Jwt jwt) { return auth.currentUser(jwt.getSubject()).id(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OrderVO create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CreateOrderRequest request) {
        return orders.create(request.productId(),userId(jwt));
    }
    @GetMapping("/my") public MyOrdersVO my(@AuthenticationPrincipal Jwt jwt) { return orders.my(userId(jwt)); }
    @GetMapping("/{id}") public OrderVO detail(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) { return orders.detail(id,userId(jwt)); }
    @PatchMapping("/{id}/pay") public OrderVO pay(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) { return orders.pay(id,userId(jwt)); }
    @PatchMapping("/{id}/ship") public OrderVO ship(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) { return orders.ship(id,userId(jwt)); }
    @PatchMapping("/{id}/complete") public OrderVO complete(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) { return orders.complete(id,userId(jwt)); }
}

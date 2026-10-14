package com.example.market.controller;
import com.example.market.dto.ProductRequest;
import com.example.market.service.ProductService;
import com.example.market.vo.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;
    public ProductController(ProductService products) { this.products=products; }
    @GetMapping public PageVO<ProductVO> list(@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="12") int size,@RequestParam(required=false) String keyword,
        @RequestParam(required=false) Long categoryId) {
        return products.list(page,size,keyword,categoryId,null);
    }
    @GetMapping("/my") public PageVO<ProductVO> my(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) {
        return products.list(page,size,null,null,products.userId(jwt.getSubject()));
    }
    @GetMapping("/{id}") public ProductDetailVO detail(@PathVariable Long id) { return products.detail(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailVO create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ProductRequest request) {
        return products.create(request,products.userId(jwt.getSubject()));
    }
    @PutMapping("/{id}") public ProductDetailVO update(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ProductRequest request) {
        return products.update(id,request,products.userId(jwt.getSubject()));
    }
    @PatchMapping("/{id}/off-shelf") public ProductDetailVO offShelf(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) {
        return products.offShelf(id,products.userId(jwt.getSubject()));
    }
}

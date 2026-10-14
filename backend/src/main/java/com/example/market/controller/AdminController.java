package com.example.market.controller;

import com.example.market.service.AdminService;
import com.example.market.vo.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** /api/admin/** 的身份、实时账号状态与ADMIN角色统一由安全过滤链校验。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    public AdminController(AdminService admin) { this.admin=admin; }
    @GetMapping("/products/pending") public PageVO<ProductVO> pending(@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="12") int size) { return admin.products(page,size,true); }
    @GetMapping("/products") public PageVO<ProductVO> products(@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="12") int size) { return admin.products(page,size,false); }
    @PatchMapping("/products/{id}/approve") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long id) { admin.review(id,true); }
    @PatchMapping("/products/{id}/reject") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long id) { admin.review(id,false); }
    @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { admin.deleteProduct(id); }
    @GetMapping("/users") public PageVO<UserVO> users(@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="12") int size) { return admin.users(page,size); }
    @PatchMapping("/users/{id}/disable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id) { admin.setUserEnabled(id,false); }
    @PatchMapping("/users/{id}/enable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable Long id) { admin.setUserEnabled(id,true); }
}

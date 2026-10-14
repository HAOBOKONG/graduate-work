package com.example.market.controller;
import com.example.market.service.CategoryService;
import com.example.market.vo.CategoryVO;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categories;
    public CategoryController(CategoryService categories) { this.categories=categories; }
    @GetMapping public List<CategoryVO> list() { return categories.list(); }
}

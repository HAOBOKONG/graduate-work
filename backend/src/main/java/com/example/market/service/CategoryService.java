package com.example.market.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.market.entity.Category;
import com.example.market.mapper.CategoryMapper;
import com.example.market.vo.CategoryVO;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class CategoryService {
    private final CategoryMapper categories;
    public CategoryService(CategoryMapper categories) { this.categories = categories; }
    public List<CategoryVO> list() {
        return categories.selectList(new QueryWrapper<Category>().orderByAsc("id")).stream()
            .map(c -> new CategoryVO(c.getId(), c.getName(), c.getParentId())).toList();
    }
}

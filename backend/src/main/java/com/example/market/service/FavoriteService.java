package com.example.market.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.market.entity.Favorite;
import com.example.market.entity.Product;
import com.example.market.entity.ProductImage;
import com.example.market.mapper.FavoriteMapper;
import com.example.market.mapper.ProductImageMapper;
import com.example.market.mapper.ProductMapper;
import com.example.market.vo.FavoriteProductVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoriteService {
    private final FavoriteMapper favorites;
    private final ProductMapper products;
    private final ProductImageMapper images;

    public FavoriteService(FavoriteMapper favorites, ProductMapper products, ProductImageMapper images) {
        this.favorites = favorites;
        this.products = products;
        this.images = images;
    }

    @Transactional
    public void add(Long userId, Long productId) {
        if (products.selectById(productId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        }
        if (favorites.selectCount(owned(userId, productId)) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "商品已收藏");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            favorites.insert(favorite);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "商品已收藏");
        }
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        if (favorites.delete(owned(userId, productId)) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏记录不存在");
        }
    }

    public List<FavoriteProductVO> my(Long userId) {
        List<Favorite> rows = favorites.selectList(new QueryWrapper<Favorite>()
            .eq("user_id", userId).orderByDesc("created_at", "id"));
        if (rows.isEmpty()) return List.of();
        List<Long> ids = rows.stream().map(Favorite::getProductId).toList();
        Map<Long, Product> byId = new HashMap<>();
        products.selectList(new QueryWrapper<Product>().in("id", ids))
            .forEach(product -> byId.put(product.getId(), product));
        Map<Long, String> firstImage = new HashMap<>();
        images.selectList(new QueryWrapper<ProductImage>().in("product_id", ids).orderByAsc("sort_order", "id"))
            .forEach(image -> firstImage.putIfAbsent(image.getProductId(), image.getImageUrl()));
        return rows.stream().map(favorite -> {
            Product product = byId.get(favorite.getProductId());
            return new FavoriteProductVO(product.getId(), product.getTitle(), product.getPrice(),
                firstImage.get(product.getId()), product.getCreatedAt());
        }).toList();
    }

    private QueryWrapper<Favorite> owned(Long userId, Long productId) {
        return new QueryWrapper<Favorite>().eq("user_id", userId).eq("product_id", productId);
    }
}

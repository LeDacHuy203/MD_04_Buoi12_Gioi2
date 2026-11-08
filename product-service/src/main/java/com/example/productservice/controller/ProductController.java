package com.example.productservice.controller;

import com.example.productservice.dto.ProductResponse;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/{id}/cache-status")
    public ResponseEntity<?> getCacheStatus(@PathVariable Long id) {
        String cacheKey = "products::" + id;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        Object cachedValue = exists ? redisTemplate.opsForValue().get(cacheKey) : null;

        return ResponseEntity.ok(Map.of(
                "productId", id,
                "cacheKey", cacheKey,
                "isCachedInRedis", exists,
                "cachedData", cachedValue != null ? cachedValue : "Không có trong cache (Cache Miss)"
        ));
    }
}

package com.example.productservice.service;

import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "products::";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    public ProductResponse getProductById(Long id) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. Kiểm tra trong Redis Cache (Cache-Aside pattern)
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            log.info("⚡ [CACHE HIT] Lấy dữ liệu sản phẩm ID={} trực tiếp từ Redis Cache (Không chạm DB)", id);
            return objectMapper.convertValue(cachedData, ProductResponse.class);
        }

        // 2. Cache Miss: Truy vấn Database
        log.info("🐢 [CACHE MISS] Đang truy vấn sản phẩm ID={} từ PostgreSQL Database...", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        ProductResponse response = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .promotionalPrice(product.getPromotionalPrice())
                .currentPrice(product.getEffectivePrice())
                .onPromotion(product.getPromotionalPrice() != null)
                .build();

        // 3. Nạp vào Redis Cache với TTL 30 phút
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        log.info("💾 [CACHE STORE] Đã nạp sản phẩm ID={} vào Redis Cache key '{}' (TTL: 30 phút)", id, cacheKey);

        return response;
    }
}

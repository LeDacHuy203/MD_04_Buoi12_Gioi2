package com.example.productservice.subscriber;

import com.example.productservice.dto.PromotionUpdateMessage;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionMessageSubscriber implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("==================================================================");
        log.info("🔔 [PRODUCT-SERVICE] [Redis Pub/Sub Subscriber] Nhận được message từ channel '{}'", channel);
        log.info("🔔 [PRODUCT-SERVICE] Nội dung thông điệp thô: {}", body);

        try {
            // Parse JSON thông điệp
            PromotionUpdateMessage updateMessage = objectMapper.readValue(body, PromotionUpdateMessage.class);
            Long productId = updateMessage.getProductId();

            log.info("🔔 [PRODUCT-SERVICE] Chi tiết: Cập nhật khuyến mãi cho Sản phẩm ID={}, Giá mới={}",
                    productId, updateMessage.getPromotionalPrice());

            // 1. Cập nhật giá khuyến mãi mới vào Database
            productRepository.findById(productId).ifPresent(product -> {
                product.setPromotionalPrice(updateMessage.getPromotionalPrice());
                productRepository.save(product);
                log.info("💾 [PRODUCT-SERVICE] Đã cập nhật giá khuyến mãi vào Database cho sản phẩm #{}", productId);
            });

            // 2. YÊU CẦU ĐỀ BÀI: Thực hiện XÓA CACHE Redis của sản phẩm đó
            String cacheKey = "products::" + productId;
            Boolean deleted = redisTemplate.delete(cacheKey);

            log.info("🧹 [PRODUCT-SERVICE] ==> ĐÃ XÓA CACHE Redis key '{}' (Kết quả: {}). " +
                    "Lần truy vấn tiếp theo bắt buộc nạp lại giá mới từ Database!", cacheKey, deleted);
            log.info("==================================================================");

        } catch (Exception e) {
            log.error("❌ [PRODUCT-SERVICE] Lỗi khi xử lý message từ Redis Pub/Sub: {}", e.getMessage(), e);
        }
    }
}

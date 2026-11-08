package com.example.promotionservice.publisher;

import com.example.promotionservice.dto.PromotionUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic promotionTopic;

    public void publishPromotionUpdate(PromotionUpdateMessage message) {
        log.info("📢 [PROMOTION-SERVICE] Đang phát thông điệp cập nhật khuyến mãi lên Redis Channel '{}': {}",
                promotionTopic.getTopic(), message);

        redisTemplate.convertAndSend(promotionTopic.getTopic(), message);

        log.info("📢 [PROMOTION-SERVICE] Gửi thành công tới channel '{}' cho ProductId: {}",
                promotionTopic.getTopic(), message.getProductId());
    }
}

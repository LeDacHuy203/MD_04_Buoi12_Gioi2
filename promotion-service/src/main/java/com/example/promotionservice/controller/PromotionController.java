package com.example.promotionservice.controller;

import com.example.promotionservice.dto.PromotionUpdateMessage;
import com.example.promotionservice.publisher.PromotionPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionPublisher promotionPublisher;

    @PostMapping("/update")
    public ResponseEntity<?> updatePromotion(@RequestBody PromotionUpdateMessage message) {
        if (message.getUpdatedAt() == null) {
            message.setUpdatedAt(LocalDateTime.now());
        }

        long start = System.currentTimeMillis();
        promotionPublisher.publishPromotionUpdate(message);
        long duration = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật khuyến mãi thành công và đã broadcast qua Redis Channel 'promotion-updates'",
                "productId", message.getProductId(),
                "promotionalPrice", message.getPromotionalPrice(),
                "broadcastTimeMs", duration
        ));
    }
}

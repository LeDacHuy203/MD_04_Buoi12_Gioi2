package com.example.productservice;

import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.PromotionUpdateMessage;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisPubSubRealtimeTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ChannelTopic promotionTopic;

    private Long productId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        // 1. Tạo sản phẩm gốc chưa có khuyến mãi
        Product product = Product.builder()
                .name("Samsung Galaxy S24 Ultra 512GB")
                .description("Flagship Snapdragon 8 Gen 3")
                .originalPrice(new BigDecimal("31990000"))
                .promotionalPrice(null)
                .build();
        Product saved = productRepository.save(product);
        productId = saved.getId();

        // Xóa cache cũ
        redisTemplate.delete("products::" + productId);
    }

    @Test
    @DisplayName("Kiểm thử cập nhật giá khuyến mãi thời gian thực với Redis Pub/Sub và xóa cache tự động (< 1s)")
    void testRealtimePromotionUpdateAndCacheInvalidation() throws InterruptedException {
        String cacheKey = "products::" + productId;

        // BƯỚC 1: Lần 1 gọi GET -> Cache Miss, đọc DB và lưu vào Redis Cache
        ProductResponse res1 = productService.getProductById(productId);
        assertThat(res1).isNotNull();
        assertThat(res1.getCurrentPrice()).isEqualByComparingTo("31990000");
        assertThat(res1.isOnPromotion()).isFalse();

        // Kiểm tra Redis đã lưu cache
        Boolean isCachedAfterFirstQuery = redisTemplate.hasKey(cacheKey);
        System.out.println("===> [BƯỚC 1] Đã lưu vào Redis Cache: " + isCachedAfterFirstQuery);
        assertThat(isCachedAfterFirstQuery).isTrue();

        // BƯỚC 2: Lần 2 gọi GET -> Cache Hit từ Redis
        ProductResponse res2 = productService.getProductById(productId);
        assertThat(res2.getCurrentPrice()).isEqualByComparingTo("31990000");

        // BƯỚC 3: Quản trị viên cập nhật giá khuyến mãi qua Promotion-Service
        // Promotion-Service phát thông điệp vào Redis Channel 'promotion-updates'
        PromotionUpdateMessage updateMessage = PromotionUpdateMessage.builder()
                .productId(productId)
                .promotionalPrice(new BigDecimal("26990000")) // Giảm giá còn 26.990.000đ
                .promotionName("Mega Sale 11.11")
                .updatedAt(LocalDateTime.now())
                .build();

        long publishTime = System.currentTimeMillis();
        redisTemplate.convertAndSend(promotionTopic.getTopic(), updateMessage);
        System.out.println("===> [BƯỚC 3] Đã phát thông điệp khuyến mãi lên Redis Channel: " + promotionTopic.getTopic());

        // BƯỚC 4: Chờ Subscriber (Product-Service) nhận tin nhắn và tự động XÓA CACHE (< 1 giây)
        Thread.sleep(400); // Chờ 400ms để Pub/Sub truyền tin
        long duration = System.currentTimeMillis() - publishTime;
        System.out.println("===> [BƯỚC 4] Thời gian truyền tin và xóa cache: " + duration + " ms");
        assertThat(duration).isLessThan(1000); // Yêu cầu đề bài: Cập nhật dưới 1 giây!

        // Khẳng định cache trong Redis ĐÃ BỊ XÓA (Cache Invalidation thành công)
        Boolean existsAfterPubSub = redisTemplate.hasKey(cacheKey);
        System.out.println("===> [BƯỚC 4] Cache key trong Redis sau khi nhận Pub/Sub message: " + existsAfterPubSub);
        assertThat(existsAfterPubSub).isFalse();

        // BƯỚC 5: Lần 3 gọi GET -> Cache Miss, tự động đọc giá khuyến mãi mới từ DB và nạp lại vào Redis
        ProductResponse res3 = productService.getProductById(productId);
        System.out.println("===> [BƯỚC 5] Giá mới nhất sau khi query lại: " + res3.getCurrentPrice() + " VNĐ");
        assertThat(res3.getCurrentPrice()).isEqualByComparingTo("26990000");
        assertThat(res3.isOnPromotion()).isTrue();

        // Khẳng định sau khi query lại, Redis Cache được nạp lại với giá mới
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
    }
}

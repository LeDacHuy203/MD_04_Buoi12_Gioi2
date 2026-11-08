package com.example.productservice.config;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            Product product = Product.builder()
                    .name("iPhone 16 Pro Max 256GB Titan Tự Nhiên")
                    .description("Điện thoại Flagship Apple A18 Pro cao cấp")
                    .originalPrice(new BigDecimal("34990000"))
                    .promotionalPrice(null) // Chưa có khuyến mãi ban đầu
                    .build();

            productRepository.save(product);
            log.info("==> [DataInitializer] Đã khởi tạo sản phẩm mẫu ID={}, Tên='{}', Giá gốc={}",
                    product.getId(), product.getName(), product.getOriginalPrice());
        }
    }
}

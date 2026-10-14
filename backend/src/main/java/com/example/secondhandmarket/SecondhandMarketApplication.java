package com.example.secondhandmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.mybatis.spring.annotation.MapperScan("com.example.market.mapper")
@SpringBootApplication(scanBasePackages = {"com.example.secondhandmarket", "com.example.market"})
public class SecondhandMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecondhandMarketApplication.class, args);
    }
}

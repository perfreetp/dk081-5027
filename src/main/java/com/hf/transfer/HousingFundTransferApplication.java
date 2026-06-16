package com.hf.transfer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("com.hf.transfer.mapper")
public class HousingFundTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(HousingFundTransferApplication.class, args);
    }
}

package com.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@MapperScan(basePackages = {"com.api.member.mapper", "com.api.item.mapper", "com.api.board.mapper", "com.api.payment.mapper", "com.api.favorite.mapper"})
public class ApiProjApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiProjApplication.class, args);
	}

}

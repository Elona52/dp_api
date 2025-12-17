package com.api.favorite.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

	private Long favoriteId; // 즐겨찾기 ID (PK)
	private String userId; // 회원 ID (FK)
	private Long itemId; // 물건 ID (FK)
	private Timestamp createdAt; // 생성일
}

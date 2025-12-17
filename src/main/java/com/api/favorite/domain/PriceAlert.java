package com.api.favorite.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert {

	private Long id;
	private Long favoriteId; // 즐겨찾기 ID (nullable - favorite 삭제되어도 히스토리 보존)
	private String memberId;
	private Long itemPlnmNo; // 물건번호 (직접 참조 - favorite 삭제되어도 조회 가능)
	private Long previousPrice; // 이전 가격
	private Long newPrice; // 새로운 가격
	private Boolean alertSent; // 알림 전송 여부
	private Timestamp sentDate; // 전송 날짜
	private Timestamp createdDate;
}

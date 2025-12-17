package com.api.item.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.api.item.dto.ItemBasic;
import com.api.item.dto.ItemDetail;

@Mapper
public interface ItemMapper {

    // 목록: 기본 정보만
    List<ItemBasic> findAllBasic();

    // 신규 목록
    List<ItemBasic> findNewItems();

    // 할인 목록
    List<ItemBasic> findDiscountItems();
    
    // 상세: 기본 + 상세 조인
    ItemDetail findDetail(Long plnmNo);
    
    // 상세: cltrMnmtNo로 조회
    ItemDetail findDetailByCltrMnmtNo(String cltrMnmtNo);

    // API 저장: 기본 정보 upsert
    int upsertItemBasic(ItemBasic item);

    // API 저장: 상세 정보 upsert
    int upsertItemDetail(ItemDetail item);
    
    // 삭제: plnmNo로 item_detail 삭제
    int deleteItemByPlnmNo(Long plnmNo);
    
    // 삭제: plnmNo로 item_basic 삭제
    int deleteItemBasicByPlnmNo(Long plnmNo);
    
    // 삭제: 서울특별시가 아닌 데이터 삭제
    int deleteNonSeoulItems();
    
    // 삭제: 전체 삭제
    int deleteAllItems();
    
    // 조회: 서울특별시 물건 조회 (페이징) - ItemDetail 반환
    List<ItemDetail> findItemsSeoul(@Param("offset") int offset, @Param("limit") int limit);
    
    // 조회: 서울특별시 물건 총 개수
    int countItemsSeoul();
    
    // 조회: 전체 물건 조회 (페이징) - ItemDetail 반환
    List<ItemDetail> findAllItems(@Param("offset") int offset, @Param("limit") int limit, @Param("category") String category);
    
    // 조회: 전체 물건 총 개수
    int countAllItems(@Param("category") String category);
    
    // 삭제: ID(plnmNo)로 삭제
    int deleteItemById(Long id);
    
    // 삭제: 물건번호(cltrMnmtNo)로 삭제
    int deleteItemByCltrNo(String cltrNo);
    
    // 조회: 신규 물건 조회 (페이징) - ItemDetail 반환 (14일 이내)
    List<ItemDetail> findNewItemsDetail(@Param("offset") int offset, @Param("limit") int limit);
    
    // 조회: 신규 물건 총 개수 (14일 이내)
    int countNewItems();
    
    // 조회: 감가 50% 이상 물건 조회 (페이징) - ItemDetail 반환
    List<ItemDetail> findDiscountItemsDetail(@Param("offset") int offset, @Param("limit") int limit);
    
    // 조회: 감가 50% 이상 물건 총 개수
    int countDiscountItems();
    
    // 조회: 오늘 마감하는 물건 조회 (경매일정용)
    List<ItemDetail> findTodayClosingItems(@Param("limit") int limit);
    
    // 조회: 카테고리별 통계 (asset_category 기반)
    List<Map<String, Object>> findCategoryStats();
    
    // 삭제: 신물건 삭제 (14일 이내 데이터)
    int deleteNewItems();
    
    // 삭제: 감가 50% 이상 물건 삭제
    int deleteDiscountItems();
    
    // 삭제: 서울특별시 전체 물건 삭제 (용도별통합물건)
    int deleteUsageItems();
}


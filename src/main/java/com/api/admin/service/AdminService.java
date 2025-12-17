package com.api.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.admin.domain.*;
import com.api.favorite.service.ServiceResponse;
import com.api.item.domain.Item;
import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;
import com.api.member.domain.Member;
import com.api.member.service.MemberService;
import com.api.union.service.ItemFetchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

	private final ItemRestService itemService;
	private final com.api.board.service.boardService boardService;
	private final MemberService memberService;
	private final ItemFetchService itemFetchService;

	/**
	 * DB에서 서울특별시 물건 조회 (페이징)
	 */
	public ItemListResponse getItemsSeoulFromDb(int page, int size) {
		try {
			List<ItemDetail> itemDetails = itemService.getItemsSeoul(page, size);
			int totalCount = itemService.countItemsSeoul();
			List<Item> items = ItemDetail.toItems(itemDetails);

			if (items == null) {
				items = new ArrayList<>();
			}

			return ItemListResponse.builder().success(true).source("DB").page(page).size(size).totalCount(totalCount)
					.currentPageCount(items.size()).items(items).message("서울특별시 물건 DB 조회 성공").build();

		} catch (Exception e) {
			log.error("❌ 서울특별시 물건 DB 조회 실패: {}", e.getMessage(), e);
			return ItemListResponse.builder().success(false).message("서울특별시 물건 DB 조회 중 오류가 발생했습니다: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}
	
	/**
	 * 용도별통합 물건 일괄 저장 (여러 페이지 순회)
	 */
	public BatchSaveResponse saveUsageItemsBatchAll(String sido) {
		try {
			log.info("💾 용도별통합 물건 일괄 저장 시작: sido={}", sido);
			int totalSaved = 0;
			int maxPages = 50;

			for (int page = 1; page <= maxPages; page++) {
				int saved = itemFetchService.fetchAndSaveUsageItems(page, sido);
				if (saved == 0) {
					log.info("💾 용도별통합 물건 저장 종료: page={}에서 데이터 없음", page);
					break;
				}
				totalSaved += saved;
				log.info("💾 용도별통합 물건 저장 진행: page={}, saved={}, total={}", page, saved, totalSaved);

				if (saved < 200) {
					log.info("💾 용도별통합 물건 저장 종료: page={}에서 200건 미만", page);
					break;
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			log.info("💾 용도별통합 물건 일괄 저장 완료: 총 {}건", totalSaved);
			return BatchSaveResponse.builder().success(true).message("용도별통합 물건 일괄 저장 완료: " + totalSaved + "건")
					.savedCount(totalSaved).totalRequested(totalSaved).build();

		} catch (Exception e) {
			log.error("❌ 용도별통합 물건 일괄 저장 실패: {}", e.getMessage(), e);
			return BatchSaveResponse.builder().success(false).message("용도별통합 물건 일괄 저장 중 오류가 발생했습니다: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	/**
	 * 전체 물건 데이터 삭제
	 */
	@Transactional
	public ServiceResponse<OperationResponse> deleteAllItemsResponse() {
		try {
			int deleted = itemService.deleteAllItems();

			log.info("✅ 전체 데이터 삭제 완료: {}건", deleted);
			OperationResponse response = OperationResponse.builder().success(true)
					.message("전체 데이터 삭제 완료: " + deleted + "건").build();
			return ServiceResponse.ok(response);
		} catch (Exception e) {
			log.error("❌ 전체 삭제 오류: {}", e.getMessage(), e);
			OperationResponse response = OperationResponse.builder().success(false)
					.message("전체 삭제 중 오류가 발생했습니다: " + e.getMessage()).build();
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}

	/**
	 * 서울특별시가 아닌 데이터 삭제
	 */
	@Transactional
	public ServiceResponse<OperationResponse> deleteNonSeoulItemsResponse() {
		try {
			int deleted = itemService.deleteNonSeoulItems();

			log.info("✅ 서울 외 지역 데이터 삭제 완료: {}건", deleted);
			OperationResponse response = OperationResponse.builder().success(true)
					.message("서울특별시가 아닌 데이터 삭제 완료: " + deleted + "건").build();
			return ServiceResponse.ok(response);

		} catch (Exception e) {
			log.error("❌ 서울 외 지역 삭제 오류: {}", e.getMessage(), e);
			OperationResponse response = OperationResponse.builder().success(false)
					.message("삭제 중 오류가 발생했습니다: " + e.getMessage()).build();
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}
	
	/**
	 * 물건 삭제 (cltrMnmtNo로)
	 */
	@Transactional
	public ServiceResponse<OperationResponse> deleteItemByCltrNoResponse(String cltrNo) {
		try {
			int deleted = itemService.deleteItemByCltrNo(cltrNo);

			log.info("✅ 물건 삭제 완료: cltrNo={}, {}건", cltrNo, deleted);
			OperationResponse response = OperationResponse.builder().success(true)
					.message("물건 삭제 완료: 물건번호 " + cltrNo).build();
			return ServiceResponse.ok(response);

		} catch (Exception e) {
			log.error("❌ 물건 삭제 오류: cltrNo={}, error={}", cltrNo, e.getMessage(), e);
			OperationResponse response = OperationResponse.builder().success(false)
					.message("삭제 중 오류가 발생했습니다: " + e.getMessage()).build();
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}
	
	/**
	 * DB에서 신규 물건 조회 (페이징) - 14일 이내
	 */
	public ItemListResponse getNewItemsFromDb(int page, int size) {
		try {
			List<ItemDetail> itemDetails = itemService.getNewItemsDetail(page, size);
			int totalCount = itemService.countNewItems();
			List<Item> items = ItemDetail.toItems(itemDetails);

			if (items == null) {
				items = new ArrayList<>();
			}

			return ItemListResponse.builder().success(true).source("DB").page(page).size(size).totalCount(totalCount)
					.currentPageCount(items.size()).items(items).message("신규 물건 DB 조회 성공").build();

		} catch (Exception e) {
			log.error("❌ 신규 물건 DB 조회 실패: {}", e.getMessage(), e);
			return ItemListResponse.builder().success(false).message("신규 물건 DB 조회 중 오류가 발생했습니다: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	/**
	 * 신물건 일괄 저장 (여러 페이지 순회)
	 */
	public BatchSaveResponse saveNewItemsBatchAll(String sido) {
		try {
			log.info("💾 신물건 일괄 저장 시작: sido={}", sido);
			int totalSaved = 0;
			int maxPages = 50;

			for (int page = 1; page <= maxPages; page++) {
				int saved = itemFetchService.fetchAndSaveNewItems(page, sido);
				if (saved == 0) {
					log.info("💾 신물건 저장 종료: page={}에서 데이터 없음", page);
					break;
				}
				totalSaved += saved;
				log.info("💾 신물건 저장 진행: page={}, saved={}, total={}", page, saved, totalSaved);

				// 200건 미만이면 더 이상 데이터가 없을 가능성이 높음
				if (saved < 200) {
					log.info("💾 신물건 저장 종료: page={}에서 200건 미만", page);
					break;
				}

				// API 호출 간격 조절
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			log.info("💾 신물건 일괄 저장 완료: 총 {}건", totalSaved);
			return BatchSaveResponse.builder().success(true).message("신물건 일괄 저장 완료: " + totalSaved + "건")
					.savedCount(totalSaved).totalRequested(totalSaved).build();

		} catch (Exception e) {
			log.error("❌ 신물건 일괄 저장 실패: {}", e.getMessage(), e);
			return BatchSaveResponse.builder().success(false).message("신물건 일괄 저장 중 오류가 발생했습니다: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	/**
	 * DB에서 감가 50% 이상 물건 조회 (페이징)
	 */
	public ItemListResponse getDiscountItemsFromDb(int page, int size) {
		try {
			List<ItemDetail> itemDetails = itemService.getDiscountItemsDetail(page, size);
			int totalCount = itemService.countDiscountItems();
			List<Item> items = ItemDetail.toItems(itemDetails);

			if (items == null) {
				items = new ArrayList<>();
			}

			return ItemListResponse.builder().success(true).source("DB").page(page).size(size).totalCount(totalCount)
					.currentPageCount(items.size()).items(items).message("감가 50% 이상 물건 DB 조회 성공").build();
		} catch (Exception e) {
			log.error("❌ 감가 50% 이상 물건 DB 조회 실패: {}", e.getMessage(), e);
			return ItemListResponse.builder().success(false)
					.message("감가 50% 이상 물건 DB 조회 중 오류가 발생했습니다: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	/**
	 * 감가50% 물건 일괄 저장 (여러 페이지 순회)
	 */
	public BatchSaveResponse saveDiscountItemsBatchAll(String sido) {
		try {
			log.info("💾 감가50% 물건 일괄 저장 시작: sido={}", sido);
			int totalSaved = 0;
			int maxPages = 50;

			for (int page = 1; page <= maxPages; page++) {
				int saved = itemFetchService.fetchAndSaveDiscountItems(page, sido);
				if (saved == 0) {
					log.info("💾 감가50% 물건 저장 종료: page={}에서 데이터 없음", page);
					break;
				}
				totalSaved += saved;
				log.info("💾 감가50% 물건 저장 진행: page={}, saved={}, total={}", page, saved, totalSaved);

				if (saved < 200) {
					log.info("💾 감가50% 물건 저장 종료: page={}에서 200건 미만", page);
					break;
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			log.info("💾 감가50% 물건 일괄 저장 완료: 총 {}건", totalSaved);
			return BatchSaveResponse.builder().success(true).message("감가50% 물건 일괄 저장 완료: " + totalSaved + "건")
					.savedCount(totalSaved).totalRequested(totalSaved).build();

		} catch (Exception e) {
			log.error("❌ 감가50% 물건 일괄 저장 실패: {}", e.getMessage(), e);
			return BatchSaveResponse.builder().success(false).message("감가50% 물건 일괄 저장 중 오류가 발생했습니다: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	

	/**
	 * 용도별 물건 API 조회 (단일 페이지)
	 */
	public ItemListResponse getItemsFromApi(boolean all, int page, int size, String sido) {
		try {
			log.info("📡 용도별 물건 API 단일 페이지 조회: sido={}, page={}, size={}", sido, page, size);
			// ItemRestService를 통해 API 호출 및 XML 파싱
			List<ItemDetail> details = itemService.fetchUsageItemsFromApi(page, sido);

			if (details == null) {
				details = List.of();
			}

			// 다음 페이지가 있는지 확인 (현재 페이지의 아이템 개수가 size와 같으면 다음 페이지가 있을 가능성)
			boolean hasNextPage = details.size() == size;
			int estimatedTotal = hasNextPage ? (page * size) + size : (page - 1) * size + details.size();

			List<Item> items = ItemDetail.toItems(details);
			return ItemListResponse.builder().success(true).source("API").page(page).size(size).sido(sido)
					.totalCount(estimatedTotal) // 추정값 (다음 페이지 확인 기반)
					.currentPageCount(details.size()).items(items).message("용도별 물건 API 조회 성공").build();
		} catch (Exception e) {
			log.error("❌ 용도별 물건 API 조회 실패: {}", e.getMessage(), e);
			return ItemListResponse.builder().success(false).message("용도별 물건 API 조회 실패: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	/**
	 * 신규 물건 API 조회 (단일 페이지)
	 */
	public ItemListResponse getNewItemsFromApi(boolean all, int page, int size, String sido) {
		try {
			log.info("📡 신규 물건 API 단일 페이지 조회: sido={}, page={}, size={}", sido, page, size);
			// ItemRestService를 통해 API 호출 및 XML 파싱
			List<ItemDetail> details = itemService.fetchNewItemsFromApi(page, sido);

			if (details == null) {
				details = List.of();
			}

			// 다음 페이지가 있는지 확인 (현재 페이지의 아이템 개수가 size와 같으면 다음 페이지가 있을 가능성)
			boolean hasNextPage = details.size() == size;
			int estimatedTotal = hasNextPage ? (page * size) + size : (page - 1) * size + details.size();

			List<Item> items = ItemDetail.toItems(details);
			return ItemListResponse.builder().success(true).source("API").page(page).size(size).sido(sido)
					.totalCount(estimatedTotal) // 추정값 (다음 페이지 확인 기반)
					.currentPageCount(details.size()).items(items).message("신규 물건 API 조회 성공").build();
		} catch (Exception e) {
			log.error("❌ 신규 물건 API 조회 실패: {}", e.getMessage(), e);
			return ItemListResponse.builder().success(false).message("신규 물건 API 조회 실패: " + e.getMessage())
					.errorType(e.getClass().getSimpleName()).build();
		}
	}

	/**
	 * 회원 조회
	 */

	public ServiceResponse<List<MemberResponse>> getAllMembersResponse() {
		try {
			List<Member> members = List.of(); // 임시로 빈 리스트
			List<MemberResponse> memberResponses = members.stream().map(MemberResponse::from).toList();
			return ServiceResponse.ok(memberResponses);
		} catch (Exception e) {
			log.error("❌ 회원 목록 조회 오류: {}", e.getMessage(), e);
			return ServiceResponse.ok(List.of());
		}
	}
	
	/**
	 * 회원 삭제
	 */
	private Member checkMemberExists(String memberId) {
		if (memberId == null || memberId.trim().isEmpty()) {
			throw new IllegalArgumentException("memberId가 비어있습니다.");
		}
		Member member = memberService.getMemberInfo(memberId);
		if (member == null) {
			throw new NoSuchElementException("해당 회원을 찾을 수 없습니다: " + memberId);
		}
		return member;
	}

	public ServiceResponse<OperationResponse> deleteMemberResponse(String memberId) {
		try {
			checkMemberExists(memberId);
			int deleted = memberService.deleteMember(memberId);
			if (deleted > 0) {
				return ServiceResponse
						.ok(OperationResponse.builder().success(true).message("회원 삭제 완료: " + memberId).build());
			} else {
				return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
						OperationResponse.builder().success(false).message("회원 삭제 실패").build());
			}
		} catch (IllegalArgumentException e) {
			return ServiceResponse.of(HttpStatus.BAD_REQUEST,
					OperationResponse.builder().success(false).message(e.getMessage()).build());
		} catch (NoSuchElementException e) {
			return ServiceResponse.of(HttpStatus.NOT_FOUND,
					OperationResponse.builder().success(false).message(e.getMessage()).build());
		} catch (Exception e) {
			log.error("❌ 회원 삭제 오류: {}", e.getMessage(), e);
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
					OperationResponse.builder().success(false).message("삭제 실패: " + e.getMessage()).build());
		}
	}

	public ServiceResponse<MemberResponse> updateMemberResponse(Map<String, String> request) {
		try {
			String memberId = request.get("memberId");
			Member member = checkMemberExists(memberId);
			if (request.containsKey("name"))
				member.setName(request.get("name"));
			if (request.containsKey("mail"))
				member.setMail(request.get("mail"));
			if (request.containsKey("phone"))
				member.setPhone(request.get("phone"));
			memberService.updateMember(member);
			return ServiceResponse.ok(MemberResponse.from(member));
		} catch (Exception e) {
			log.error("❌ 회원 정보 수정 실패: {}", e.getMessage(), e);
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, (MemberResponse) null);
		}
	}

	// =============================================================================
	// 게시판 관리
	// =============================================================================

	/**
	 * 전체 게시글 목록 조회
	 */
	public ServiceResponse<List<Map<String, Object>>> getAllBoardsResponse() {
		try {
			List<com.api.board.domain.FindBoard> boards = boardService.getBoardList(null, null, null);

			List<Map<String, Object>> boardResponses = boards.stream().map(board -> {
				Map<String, Object> map = new HashMap<>();
				map.put("no", board.getNo());
				map.put("id", board.getId());
				map.put("title", board.getTitle());
				map.put("content", board.getContent());
				map.put("category", board.getCategory());
				map.put("views", board.getViews());
				map.put("relatedLink", board.getRelatedLink());
				map.put("regDate", board.getRegDate());
				return map;
			}).toList();

			log.info("✅ 게시글 목록 조회 성공: {}개", boardResponses.size());
			return ServiceResponse.ok(boardResponses);

		} catch (Exception e) {
			log.error("❌ 게시글 목록 조회 오류: {}", e.getMessage(), e);
			return ServiceResponse.ok(List.of());
		}
	}

	/**
	 * 게시글 삭제
	 */
	@Transactional
	public ServiceResponse<OperationResponse> deleteBoardResponse(int boardNo) {
		try {
			com.api.board.domain.FindBoard board = boardService.getBoard(boardNo);
			if (board == null) {
				OperationResponse response = OperationResponse.builder().success(false)
						.message("해당 게시글을 찾을 수 없습니다: " + boardNo).build();
				return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
			}

			boardService.deleteBoard(boardNo);

			log.info("✅ 게시글 삭제 성공: boardNo={}", boardNo);
			OperationResponse response = OperationResponse.builder().success(true).message("게시글 삭제 완료: 번호 " + boardNo)
					.build();
			return ServiceResponse.ok(response);

		} catch (Exception e) {
			log.error("❌ 게시글 삭제 오류: boardNo={}, error={}", boardNo, e.getMessage(), e);
			OperationResponse response = OperationResponse.builder().success(false).message("삭제 실패: " + e.getMessage())
					.build();
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}
}

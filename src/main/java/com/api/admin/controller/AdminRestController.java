package com.api.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import com.api.admin.domain.BatchSaveResponse;
import com.api.admin.domain.ItemListResponse;
import com.api.admin.domain.MemberResponse;
import com.api.admin.domain.OperationResponse;
import com.api.admin.service.AdminService;
import com.api.union.service.ApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRestController {

	private final AdminService adminService;
	private final ApiService apiService;

	// =============================================================================
	// 관리 페이지 (View)
	// =============================================================================

	/**
	 * 관리 페이지 화면 GET /api/admin/panel
	 */
	@GetMapping("/panel")
	public ModelAndView adminPanel() {
		log.info("🌐 [URL 호출] GET /api/admin/panel");
		return new ModelAndView("admin/admin-panel");
	}

	/**
	 * 헬스 체크 엔드포인트 (404 오류 확인용) GET /api/admin/health
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> healthCheck() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "OK");
		response.put("message", "AdminRestController is working");
		response.put("timestamp", System.currentTimeMillis());
		log.info("✅ Health check: AdminRestController is accessible");
		return ResponseEntity.ok(response);
	}

	/**
	 * 용도별 물건 API 목록 조회 (전체) - AdminService를 통해 전체 데이터 가져오기 GET
	 * /api/admin/api/items?all=true&sido=서울특별시
	 */
	@GetMapping("/api/items")
	public ResponseEntity<ItemListResponse> getItemsFromApi(
			@RequestParam(name = "all", defaultValue = "false") boolean all,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "100") int size,
			@RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {

		ItemListResponse response = adminService.getItemsFromApi(all, page, size, sido);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	// =============================================================================
	// 원본 API 응답 조회 (ApiService 직접 호출)
	// =============================================================================

	/**
	 * 신물건 원본 API 응답 조회 (XML) GET /api/admin/api-raw/new-items?page=1&sido=서울특별시
	 */
	@GetMapping("/api-raw/new-items")
	public ResponseEntity<Map<String, Object>> getNewItemsRawApi(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {

		log.info("📡 원본 API 호출: 신물건 조회 - page={}, sido={}", page, sido);

		try {
			String xmlResponse = apiService.getUnifyNewCltrList(page, 200, sido);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("page", page);
			response.put("sido", sido);
			response.put("xmlResponse", xmlResponse);
			response.put("message", "원본 API 응답 조회 성공");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("❌ 원본 API 호출 실패: {}", e.getMessage(), e);
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "원본 API 호출 실패: " + e.getMessage());
			response.put("errorType", e.getClass().getSimpleName());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	/**
	 * 감가 50% 이상 원본 API 응답 조회 (XML) GET
	 * /api/admin/api-raw/discount-items?page=1&sido=서울특별시
	 */
	@GetMapping("/api-raw/discount-items")
	public ResponseEntity<Map<String, Object>> getDiscountItemsRawApi(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {

		log.info("📡 원본 API 호출: 감가 50% 이상 조회 - page={}, sido={}", page, sido);

		try {
			String xmlResponse = apiService.getUnifyDegression50PerCltrList(page, 200, sido);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("page", page);
			response.put("sido", sido);
			response.put("xmlResponse", xmlResponse);
			response.put("message", "원본 API 응답 조회 성공");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("❌ 원본 API 호출 실패: {}", e.getMessage(), e);
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "원본 API 호출 실패: " + e.getMessage());
			response.put("errorType", e.getClass().getSimpleName());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	/**
	 * 용도별 통합 원본 API 응답 조회 (XML) GET
	 * /api/admin/api-raw/usage-items?page=1&sido=서울특별시
	 */
	@GetMapping("/api-raw/usage-items")
	public ResponseEntity<Map<String, Object>> getUsageItemsRawApi(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {

		log.info("📡 원본 API 호출: 용도별 통합 조회 - page={}, sido={}", page, sido);

		try {
			String xmlResponse = apiService.getUnifyUsageCltrList(page, 200, sido);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("page", page);
			response.put("sido", sido);
			response.put("xmlResponse", xmlResponse);
			response.put("message", "원본 API 응답 조회 성공");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("❌ 원본 API 호출 실패: {}", e.getMessage(), e);
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "원본 API 호출 실패: " + e.getMessage());
			response.put("errorType", e.getClass().getSimpleName());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	// =============================================================================
	// API 데이터를 DB에 저장
	// =============================================================================

	/**
	 * DB에서 신규 물건 조회 (페이징) - 14일 이내 GET /api/admin/db/new-items?page=1&size=100
	 */
	@GetMapping("/db/new-items")
	public ResponseEntity<ItemListResponse> getNewItemsFromDb(@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "100") int size) {

		log.info("📊 DB 신규 물건 조회: page={}, size={}", page, size);

		ItemListResponse response = adminService.getNewItemsFromDb(page, size);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	// =============================================================================
	// DB 조회 (물건)
	// =============================================================================

	/**
	 * DB에서 서울특별시 물건 조회 (페이징) GET /api/admin/db/items-seoul?page=1&size=50
	 */
	@GetMapping("/db/items-seoul")
	public ResponseEntity<ItemListResponse> getItemsSeoulFromDb(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "50") int size) {

		log.info("📊 DB 서울특별시 물건 조회: page={}, size={}", page, size);

		ItemListResponse response = adminService.getItemsSeoulFromDb(page, size);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * DB에서 감가 50% 이상 물건 조회 (페이징) GET /api/admin/db/discount-items?page=1&size=200
	 */
	@GetMapping("/db/discount-items")
	public ResponseEntity<ItemListResponse> getDiscountItemsFromDb(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "200") int size) {

		log.info("📊 DB 감가 50% 이상 물건 조회: page={}, size={}", page, size);

		ItemListResponse response = adminService.getDiscountItemsFromDb(page, size);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	// =============================================================================
	// 데이터 삭제
	// =============================================================================
	/**
	 * DB에서 서울특별시 외 지역 삭제 DELETE /api/admin/db/delete-non-seoul
	 */
	@DeleteMapping("/db/delete-non-seoul")
	public ResponseEntity<OperationResponse> deleteNonSeoulItems() {
		log.info("========================================");
		log.info("🌐 [URL 호출] DELETE /api/admin/db/delete-non-seoul");
		log.info("🗑️ [대량 삭제 시작] 서울특별시 외 지역 삭제");

		return adminService.deleteNonSeoulItemsResponse().toResponseEntity();
	}

	/**
	 * DB에서 전체 데이터 삭제 (위험!) DELETE /api/admin/db/delete-all
	 */
	@DeleteMapping("/db/delete-all")
	public ResponseEntity<OperationResponse> deleteAllItems() {
		log.info("========================================");
		log.info("🌐 [URL 호출] DELETE /api/admin/db/delete-all");
		log.info("⚠️⚠️⚠️ [전체 삭제 시작] 모든 데이터 삭제 ⚠️⚠️⚠️");

		return adminService.deleteAllItemsResponse().toResponseEntity();
	}

	/**
	 * DB에서 물건 삭제 (cltrMnmtNo로) DELETE /api/admin/db/item-by-cltr/{cltrNo}
	 */
	@DeleteMapping("/db/item-by-cltr/{cltrNo}")
	public ResponseEntity<OperationResponse> deleteItemByCltrNo(@PathVariable("cltrNo") String cltrNo) {
		log.info("========================================");
		log.info("🌐 [URL 호출] DELETE /api/admin/db/item-by-cltr/{}", cltrNo);
		log.info("🗑️ [물건 삭제 시작] 물건번호: {}", cltrNo);

		return adminService.deleteItemByCltrNoResponse(cltrNo).toResponseEntity();
	}

	/**
	 * 신규 물건 API 목록 조회 (전체) - AdminService를 통해 전체 데이터 가져오기 GET
	 * /api/admin/api/new-items?all=true&sido=서울특별시 GET
	 * /api/admin/api/new-items?page=1&size=100&sido=서울특별시 (단일 페이지)
	 */
	@GetMapping("/api/new-items")
	public ResponseEntity<ItemListResponse> getNewItemsFromApi(
			@RequestParam(name = "all", defaultValue = "false") boolean all,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "100") int size,
			@RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {

		ItemListResponse response = adminService.getNewItemsFromApi(all, page, size, sido);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 신물건 일괄 저장 (여러 페이지 순회) POST /api/admin/save-batch-new-items Body: { "sido":
	 * "서울특별시" }
	 */
	@PostMapping("/save-batch-new-items")
	public ResponseEntity<BatchSaveResponse> saveNewItemsBatchAll(@RequestBody Map<String, Object> request) {
		String sido = (String) request.getOrDefault("sido", "서울특별시");
		log.info("💾 신물건 일괄 저장 요청: sido={}", sido);

		BatchSaveResponse response = adminService.saveNewItemsBatchAll(sido);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 감가50% 물건 일괄 저장 (여러 페이지 순회) POST /api/admin/save-batch-discount-items Body: {
	 * "sido": "서울특별시" }
	 */
	@PostMapping("/save-batch-discount-items")
	public ResponseEntity<BatchSaveResponse> saveDiscountItemsBatchAll(@RequestBody Map<String, Object> request) {
		String sido = (String) request.getOrDefault("sido", "서울특별시");
		log.info("💾 감가50% 물건 일괄 저장 요청: sido={}", sido);

		BatchSaveResponse response = adminService.saveDiscountItemsBatchAll(sido);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 용도별통합 물건 일괄 저장 (여러 페이지 순회) POST /api/admin/save-batch-usage-items Body: {
	 * "sido": "서울특별시" }
	 */
	@PostMapping("/save-batch-usage-items")
	public ResponseEntity<BatchSaveResponse> saveUsageItemsBatchAll(@RequestBody Map<String, Object> request) {
		String sido = (String) request.getOrDefault("sido", "서울특별시");
		log.info("💾 용도별통합 물건 일괄 저장 요청: sido={}", sido);

		BatchSaveResponse response = adminService.saveUsageItemsBatchAll(sido);

		if (!response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		return ResponseEntity.ok(response);
	}

	// =============================================================================
	// 회원 관리
	// =============================================================================

	/**
	 * 전체 회원 목록 조회 GET /api/members/all
	 */
	@GetMapping("/members/all")
	public ResponseEntity<List<MemberResponse>> getAllMembers() {
		log.info("========================================");
		log.info("🌐 [URL 호출] GET /api/admin/members/all");
		log.info("👥 [회원 조회 시작] 전체 회원 목록 조회");

		return adminService.getAllMembersResponse().toResponseEntity();
	}

	// ----------------------------
	// 회원 삭제
	// ----------------------------
	@DeleteMapping("/members/{memberId}")
	public ResponseEntity<OperationResponse> deleteMember(@PathVariable String memberId) {
		log.info("🌐 [회원 삭제] memberId={}", memberId);
		// ServiceResponse가 이미 상태 처리함
		return adminService.deleteMemberResponse(memberId).toResponseEntity();
	}

	// ----------------------------
	// 회원 정보 수정
	// ----------------------------
	@PostMapping("/members/update")
	public ResponseEntity<MemberResponse> updateMember(@RequestBody Map<String, String> request) {
		log.info("🌐 [회원 정보 수정] request={}", request);
		// ServiceResponse가 이미 상태 처리함
		return adminService.updateMemberResponse(request).toResponseEntity();
	}

	// =============================================================================
	// 게시판 관리
	// =============================================================================

	/**
	 * 전체 게시글 목록 조회 GET /api/admin/boards/all
	 */
	@GetMapping("/boards/all")
	public ResponseEntity<List<Map<String, Object>>> getAllBoards() {
		log.info("========================================");
		log.info("🌐 [URL 호출] GET /api/admin/boards/all");
		log.info("📝 [게시글 조회 시작] 전체 게시글 목록 조회");

		return adminService.getAllBoardsResponse().toResponseEntity();
	}

	/**
	 * 게시글 삭제 DELETE /api/admin/boards/{boardNo}
	 */
	@DeleteMapping("/boards/{boardNo}")
	public ResponseEntity<OperationResponse> deleteBoard(@PathVariable("boardNo") int boardNo) {
		log.info("========================================");
		log.info("🌐 [URL 호출] DELETE /api/admin/boards/{}", boardNo);
		log.info("🗑️ [게시글 삭제 시작] 게시글 번호: {}", boardNo);

		return adminService.deleteBoardResponse(boardNo).toResponseEntity();
	}

}

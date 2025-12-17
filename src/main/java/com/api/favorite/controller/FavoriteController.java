package com.api.favorite.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.favorite.service.FavoriteService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 즐겨찾기 추가 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addFavorite(HttpSession session,
                                                           @RequestBody Map<String, Object> requestBody) {
        String userId = (String) session.getAttribute("loginId");
        return favoriteService.handleAddFavoriteRequest(userId, requestBody).toResponseEntity();
    }

    /** 즐겨찾기 삭제 */
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(HttpSession session,
                                                              @PathVariable Long favoriteId) {
        String userId = (String) session.getAttribute("loginId");
        return favoriteService.handleRemoveFavoriteRequest(userId, favoriteId).toResponseEntity();
    }

    /** 내 즐겨찾기 목록 조회 */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyFavorites(HttpSession session) {
        String userId = (String) session.getAttribute("loginId");
        return favoriteService.handleFavoritesResponse(userId).toResponseEntity();
    }

    /** 특정 물건 즐겨찾기 여부 확인 */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkFavorite(HttpSession session,
                                                             @RequestParam(required = false) Long itemId,
                                                             @RequestParam(required = false) String cltrNo,
                                                             @RequestParam(required = false) String itemPlnmNo) {
        String userId = (String) session.getAttribute("loginId");
        return favoriteService.handleFavoriteCheck(userId, itemId, cltrNo, itemPlnmNo).toResponseEntity();
    }
}


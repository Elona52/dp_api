package com.api.info.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class InfoController {
	
	// 경매과정 안내페이지
		@GetMapping("/information")
		public String information() {
			return "info/information";
		}
		
		// 입찰참가 안내 페이지
		@GetMapping("/bidding-guide")
		public String biddingGuide() {
			return "info/bidding-guide";
		}
		
		// 낙찰 후 절차 안내 페이지 (압류재산)
		@GetMapping("/post-bid-procedure")
		public String postBidProcedure() {
			return "info/post-bid-procedure";
		}
		
		// 국유재산 낙찰 후 절차 안내 페이지
		@GetMapping("/national-property-procedure")
		public String nationalPropertyProcedure() {
			return "info/national-property-procedure";
		}
		
		// 수탁재산, 유입·유동화자산 낙찰 후 절차 안내 페이지
		@GetMapping("/entrusted-property-procedure")
		public String entrustedPropertyProcedure() {
			return "info/entrusted-property-procedure";
		}
		
		// 사이트맵 페이지
		@GetMapping("/sitemap")
		public String sitemap() {
			return "sitemap";
		}
		
		// 가격 알림 페이지
		@GetMapping("/price-alerts")
		public String priceAlerts(HttpSession session) {
			String userId = (String) session.getAttribute("loginId");
			if (userId == null || userId.isEmpty()) {
				return "redirect:/memberLogin";
			}
			return "payment/price-alerts";
		}
		
		// 관리자 회원 관리 페이지
		@GetMapping("/adminMemberRegister")
		public String adminMemberRegister(HttpSession session) {
			// 관리자 권한 확인
			String userType = (String) session.getAttribute("type");
			if (userType == null || !"admin".equals(userType)) {
				return "redirect:/main";
			}
			return "redirect:/api/admin/panel";
		}
		
		// 관리자 패널 (기존 URL 호환성)
		@GetMapping("/admin/panel")
		public String adminPanel(HttpSession session) {
			// 관리자 권한 확인
			String userType = (String) session.getAttribute("type");
			if (userType == null || !"admin".equals(userType)) {
				return "redirect:/main";
			}
			return "redirect:/api/admin/panel";
		}

}

package com.api.member.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.api.member.domain.Member;
import com.api.member.dto.MemberJoinRequest;
import com.api.member.dto.MemberLoginRequest;
import com.api.member.dto.MemberUpdateRequest;
import com.api.member.service.MemberService;
import com.api.member.service.ServiceResult;

@Controller
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService memberService;

	// 로그인 페이지 (GET: /memberLogin)
	@GetMapping("/memberLogin")
	public String loginPage() {
		return "member/login";
	}

	// 로그아웃 처리 (GET: /logout)
	@GetMapping("/logout")
	public String logout(HttpSession session) {
		memberService.logout(session);
		return "redirect:/main";
	}

	// 아이디 찾기 페이지 (GET: /findId)
	@GetMapping("/findId")
	public String findIdPage() {
		return "member/findId";
	}

	// 비밀번호 찾기 페이지 (GET: /findPassword)
	@GetMapping("/findPassword")
	public String findPasswordPage() {
		return "member/findPassword";
	}

	// 회원가입 페이지 (GET: /memberJoin)
	@GetMapping("/memberJoin")
	public String joinPage() {
		return "member/join";
	}

	// 회원가입 처리 (폼 액션: /memberJoin)
	@PostMapping("/memberJoin")
	public String joinSubmit(@ModelAttribute MemberJoinRequest request,
	                         HttpSession session,
	                         Model model) {
	    ServiceResult<Member> result = memberService.join(request, session);
	    model.addAttribute("message", result.getMessage());
	    return result.isSuccess() ? "redirect:/main" : "member/join";
	}

	// 로그인 처리 (폼 액션: /login)
	@PostMapping("/login")
	public String login(@ModelAttribute MemberLoginRequest request,
	                    HttpSession session,
	                    Model model) {
	    ServiceResult<Member> result = memberService.login(request, session);
	    model.addAttribute("message", result.getMessage());
	    return result.isSuccess() ? "redirect:/main" : "member/login";
	}

	// 회원정보 수정 페이지 (GET: /memberUpdate)
	@GetMapping("/memberUpdate")
	public String updatePage(HttpSession session, Model model) {
		Boolean isLogin = (Boolean) session.getAttribute("isLogin");
		String loginId = (String) session.getAttribute("loginId");
		
		// 로그인 안 되어 있으면 로그인 페이지로
		if (isLogin == null || !isLogin || loginId == null) {
			return "redirect:/memberLogin";
		}
		
		// 회원 정보 조회
		ServiceResult<Member> memberResult = memberService.getMemberInfoResponse(loginId);
		if (memberResult.isSuccess() && memberResult.getData() != null) {
			model.addAttribute("member", memberResult.getData());
		}
		
		return "member/join";
	}
	
	// 회원정보 수정 (폼 액션: /memberUpdate)
	@PostMapping("/memberUpdate")
	public String updateSubmit(@ModelAttribute MemberUpdateRequest request,
	                           Model model) {
	    ServiceResult<Member> result = memberService.update(request);
	    model.addAttribute("message", result.getMessage());
	    return result.isSuccess() ? "redirect:/main" : "member/join";
	}

	// 회원정보 조회 (AJAX: /getMemberInfo)
	@PostMapping("/getMemberInfo")
	@ResponseBody
	public ServiceResult<Member> getMemberInfo(@RequestParam("id") String id) {
		ServiceResult<Member> result = memberService.getMemberInfoResponse(id);
		return result;
	}

	// 아이디 중복체크 (AJAX: /idCheck)
	@PostMapping("/idCheck")
	@ResponseBody
	public ServiceResult<Boolean> idCheck(@RequestParam("id") String id) {
		ServiceResult<Boolean> result = memberService.idCheckResponse(id);
		return result;
	}

	// 비밀번호 확인 (AJAX: /isPass)
	@PostMapping("/isPass")
	@ResponseBody
	public ServiceResult<Boolean> isPass(@RequestParam("id") String id,
	                                     @RequestParam("pass") String pass) {
		ServiceResult<Boolean> result = memberService.isPassResponse(id, pass);
		return result;
	}

	// 아이디 찾기 (POST: /findId)
	@PostMapping("/findId")
	@ResponseBody
	public ServiceResult<String> findId(@RequestParam("name") String name,
	                                    @RequestParam("mobile1") String mobile1,
	                                    @RequestParam("mobile2") String mobile2) {
		// 전화번호 합치기
		String phone = mobile1 + mobile2;
		return memberService.findId(name, phone);
	}

	// 비밀번호 찾기 (회원 정보 확인) (POST: /findPassword)
	@PostMapping("/findPassword")
	@ResponseBody
	public ServiceResult<Boolean> findPassword(@RequestParam("id") String id,
	                                           @RequestParam("name") String name,
	                                           @RequestParam("mobile1") String mobile1,
	                                           @RequestParam("mobile2") String mobile2) {
		// 전화번호 합치기
		String phone = mobile1 + mobile2;
		return memberService.findPassword(id, name, phone);
	}

	// 비밀번호 재설정 (POST: /resetPassword)
	@PostMapping("/resetPassword")
	@ResponseBody
	public ServiceResult<Boolean> resetPassword(@RequestParam("id") String id,
	                                           @RequestParam("newPassword") String newPassword) {
		return memberService.resetPassword(id, newPassword);
	}

	// 즐겨찾기 페이지 (GET: /myFavorites)
	@GetMapping("/myFavorites")
	public String favoritesPage() {
		return "member/favorites";
	}

	// 마이페이지 - 예약 목록 (GET: /mypage/reservations)
	@GetMapping("/mypage/reservations")
	public String mypageReservations(HttpSession session, Model model) {
		if (session.getAttribute("isLogin") == null) {
			return "redirect:/memberLogin";
		}
		String loginId = (String) session.getAttribute("loginId");
		ServiceResult<Member> memberResult = memberService.getMemberInfoResponse(loginId);
		if (memberResult.isSuccess()) {
			model.addAttribute("member", memberResult.getData());
		}
		return "member/mypage-reservations";
	}

	// 마이페이지 - 이용 현황 (GET: /mypage/usage)
	@GetMapping("/mypage/usage")
	public String mypageUsage(HttpSession session, Model model) {
		if (session.getAttribute("isLogin") == null) {
			return "redirect:/memberLogin";
		}
		String loginId = (String) session.getAttribute("loginId");
		ServiceResult<Member> memberResult = memberService.getMemberInfoResponse(loginId);
		if (memberResult.isSuccess()) {
			model.addAttribute("member", memberResult.getData());
		}
		return "member/mypage-usage";
	}
	
	// 나의 응찰목록 (GET: /myAuctionList)
	@GetMapping("/myAuctionList")
	public String myAuctionList(HttpSession session) {
		Boolean isLogin = (Boolean) session.getAttribute("isLogin");
		String loginId = (String) session.getAttribute("loginId");
		
		// 로그인 안 되어 있으면 로그인 페이지로
		if (isLogin == null || !isLogin || loginId == null) {
			return "redirect:/memberLogin";
		}
		
		// 응찰목록은 구매내역 페이지로 리다이렉트
		return "redirect:/payment/my-payments";
	}
}


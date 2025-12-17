package com.api.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.member.domain.Member;
import com.api.member.dto.MemberJoinRequest;
import com.api.member.dto.MemberLoginRequest;
import com.api.member.dto.MemberUpdateRequest;
import com.api.member.service.MemberService;
import com.api.member.service.ServiceResult;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/member")
public class MemberRestController {

    @Autowired
    private MemberService memberService;

    /**
     * 회원가입 API
     * POST /api/member/join
     */
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody MemberJoinRequest request, HttpSession session) {
        ServiceResult<Member> result = memberService.join(request, session);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok()
                .body(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, result.getMessage(), null));
        }
    }

    /**
     * 로그인 API
     * POST /api/member/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberLoginRequest request, HttpSession session) {
        ServiceResult<Member> result = memberService.login(request, session);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok()
                .body(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, result.getMessage(), null));
        }
    }

    /**
     * 회원정보 수정 API
     * POST /api/member/update
     */
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody MemberUpdateRequest request) {
        ServiceResult<Member> result = memberService.update(request);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok()
                .body(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, result.getMessage(), null));
        }
    }

    /**
     * 회원정보 조회 API
     * GET /api/member/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMemberInfo(@PathVariable("id") String id) {
        Member member = memberService.getMemberInfo(id);
        
        if (member != null) {
            // 비밀번호는 제외하고 반환
            member.setPass(null);
            return ResponseEntity.ok()
                .body(new ApiResponse(true, "회원정보 조회 성공", member));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "회원을 찾을 수 없습니다.", null));
        }
    }

    /**
     * 아이디 중복체크 API
     * GET /api/member/check-id/{id}
     */
    @GetMapping("/check-id/{id}")
    public ResponseEntity<?> checkId(@PathVariable("id") String id) {
        boolean available = memberService.idCheck(id);
        
        return ResponseEntity.ok()
            .body(new ApiResponse(true, available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.", 
                new IdCheckResponse(id, available)));
    }

    /**
     * 로그아웃 API
     * POST /api/member/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok()
            .body(new ApiResponse(true, "로그아웃되었습니다.", null));
    }

    // 응답 클래스
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class IdCheckResponse {
        private String id;
        private boolean available;

        public IdCheckResponse(String id, boolean available) {
            this.id = id;
            this.available = available;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}


package com.api.member.service;

import com.api.member.domain.Member;
import com.api.member.dto.MemberJoinRequest;
import com.api.member.dto.MemberLoginRequest;
import com.api.member.dto.MemberUpdateRequest;
import com.api.member.mapper.MemberMapper;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

// ServiceResult는 같은 패키지에 있으므로 import 불필요

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // --------------------------
    // 회원가입 처리
    // --------------------------
    @Transactional
    public ServiceResult<Member> join(MemberJoinRequest request, HttpSession session) {
        try {
            if(request.getPass() == null || request.getPass().trim().isEmpty()) {
                return new ServiceResult<>(false, "비밀번호를 입력해주세요.", null);
            }

            if(!idCheck(request.getId())) {
                return new ServiceResult<>(false, "이미 사용 중인 아이디입니다.", null);
            }

            Member member = request.toMember();
            member.setPass(passwordEncoder.encode(member.getPass()));
            member.setType("USER");
            memberMapper.insertMember(member);

            // 가입 후 자동 로그인
            session.setAttribute("isLogin", true);
            session.setAttribute("loginId", member.getId());
            session.setAttribute("type", member.getType());

            return new ServiceResult<>(true, "회원가입 성공", member);
        } catch (Exception e) {
            return new ServiceResult<>(false, "회원가입 중 오류가 발생했습니다: " + e.getMessage(), null);
        }
    }

    // --------------------------
    // 로그인 처리
    // --------------------------
    @Transactional(readOnly = true)
    public ServiceResult<Member> login(MemberLoginRequest request, HttpSession session) {
        Member member = memberMapper.getMemberInfo(request.getId());
        if(member == null) {
            return new ServiceResult<>(false, "아이디가 존재하지 않습니다.", null);
        }
        if(!passwordEncoder.matches(request.getPass(), member.getPass())) {
            return new ServiceResult<>(false, "비밀번호가 일치하지 않습니다.", null);
        }

        session.setAttribute("isLogin", true);
        session.setAttribute("loginId", member.getId());
        session.setAttribute("type", member.getType());

        return new ServiceResult<>(true, "로그인 성공", member);
    }

    // --------------------------
    // 회원정보 조회
    // --------------------------
    @Transactional(readOnly = true)
    public Member getMemberInfo(String id) {
        return memberMapper.getMemberInfo(id);
    }

    // --------------------------
    // 회원정보 수정
    // --------------------------
    @Transactional
    public ServiceResult<Member> update(MemberUpdateRequest request) {
        try {
            Member member = request.toMember();
            if(request.getPass() != null && !request.getPass().isEmpty()) {
                member.setPass(passwordEncoder.encode(request.getPass()));
            }
            memberMapper.updateMember(member);
            return new ServiceResult<>(true, "회원정보 수정 성공", member);
        } catch(Exception e) {
            return new ServiceResult<>(false, "회원정보 수정 실패: " + e.getMessage(), null);
        }
    }

    // --------------------------
    // 아이디 중복체크
    // --------------------------
    @Transactional(readOnly = true)
    public boolean idCheck(String id) {
        return memberMapper.getMemberInfo(id) == null;
    }

    // --------------------------
    // 비밀번호 확인
    // --------------------------
    @Transactional(readOnly = true)
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // --------------------------
    // 회원정보 조회 (ServiceResult 반환)
    // --------------------------
    @Transactional(readOnly = true)
    public ServiceResult<Member> getMemberInfoResponse(String id) {
        try {
            Member member = memberMapper.getMemberInfo(id);
            if (member == null) {
                return new ServiceResult<>(false, "회원을 찾을 수 없습니다.", null);
            }
            // 비밀번호는 제외하고 반환
            member.setPass(null);
            return new ServiceResult<>(true, "회원정보 조회 성공", member);
        } catch (Exception e) {
            return new ServiceResult<>(false, "회원정보 조회 중 오류가 발생했습니다: " + e.getMessage(), null);
        }
    }

    // --------------------------
    // 아이디 중복체크 (ServiceResult 반환)
    // --------------------------
    @Transactional(readOnly = true)
    public ServiceResult<Boolean> idCheckResponse(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return new ServiceResult<>(false, "아이디를 입력해주세요.", false);
            }
            boolean available = memberMapper.getMemberInfo(id) == null;
            String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
            return new ServiceResult<>(true, message, available);
        } catch (Exception e) {
            return new ServiceResult<>(false, "중복체크 중 오류가 발생했습니다: " + e.getMessage(), false);
        }
    }

    // --------------------------
    // 비밀번호 확인 (ServiceResult 반환)
    // --------------------------
    @Transactional(readOnly = true)
    public ServiceResult<Boolean> isPassResponse(String id, String pass) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return new ServiceResult<>(false, "아이디를 입력해주세요.", false);
            }
            if (pass == null || pass.trim().isEmpty()) {
                return new ServiceResult<>(false, "비밀번호를 입력해주세요.", false);
            }

            Member member = memberMapper.getMemberInfo(id);
            if (member == null) {
                return new ServiceResult<>(false, "아이디가 존재하지 않습니다.", false);
            }

            boolean matches = passwordEncoder.matches(pass, member.getPass());
            String message = matches ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다.";
            return new ServiceResult<>(true, message, matches);
        } catch (Exception e) {
            return new ServiceResult<>(false, "비밀번호 확인 중 오류가 발생했습니다: " + e.getMessage(), false);
        }
    }

    // --------------------------
    // 회원 삭제
    // --------------------------
    @Transactional
    public int deleteMember(String id) {
        return memberMapper.deleteMember(id);
    }

    // --------------------------
    // 회원 정보 수정 (관리자용 - 비밀번호 암호화 없이)
    // --------------------------
    @Transactional
    public void updateMember(Member member) {
        memberMapper.updateMember(member);
    }

    // --------------------------
    // 아이디 찾기 (이름, 전화번호로 조회)
    // --------------------------
    @Transactional(readOnly = true)
    public ServiceResult<String> findId(String name, String phone) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return new ServiceResult<>(false, "이름을 입력해주세요.", null);
            }
            if (phone == null || phone.trim().isEmpty()) {
                return new ServiceResult<>(false, "전화번호를 입력해주세요.", null);
            }
            
            // 전화번호 형식 정규화 (하이픈 제거, 공백 제거)
            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            
            Member member = memberMapper.findMemberByNameAndPhone(name, normalizedPhone);
            if (member == null) {
                return new ServiceResult<>(false, "입력하신 정보와 일치하는 회원을 찾을 수 없습니다.", null);
            }
            
            return new ServiceResult<>(true, "아이디를 찾았습니다.", member.getId());
        } catch (Exception e) {
            return new ServiceResult<>(false, "아이디 찾기 중 오류가 발생했습니다: " + e.getMessage(), null);
        }
    }

    // --------------------------
    // 비밀번호 찾기 (회원 정보 확인)
    // --------------------------
    @Transactional(readOnly = true)
    public ServiceResult<Boolean> findPassword(String id, String name, String phone) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return new ServiceResult<>(false, "아이디를 입력해주세요.", false);
            }
            if (name == null || name.trim().isEmpty()) {
                return new ServiceResult<>(false, "이름을 입력해주세요.", false);
            }
            if (phone == null || phone.trim().isEmpty()) {
                return new ServiceResult<>(false, "전화번호를 입력해주세요.", false);
            }
            
            // 전화번호 형식 정규화 (하이픈 제거, 공백 제거)
            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            
            Member member = memberMapper.findMemberByIdNameAndPhone(id, name, normalizedPhone);
            if (member == null) {
                return new ServiceResult<>(false, "입력하신 정보와 일치하는 회원을 찾을 수 없습니다.", false);
            }
            
            return new ServiceResult<>(true, "회원 정보가 확인되었습니다.", true);
        } catch (Exception e) {
            return new ServiceResult<>(false, "비밀번호 찾기 중 오류가 발생했습니다: " + e.getMessage(), false);
        }
    }

    // --------------------------
    // 비밀번호 재설정
    // --------------------------
    @Transactional
    public ServiceResult<Boolean> resetPassword(String id, String newPassword) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return new ServiceResult<>(false, "아이디를 입력해주세요.", false);
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return new ServiceResult<>(false, "새 비밀번호를 입력해주세요.", false);
            }
            
            // 비밀번호 길이 검증
            if (newPassword.length() < 4) {
                return new ServiceResult<>(false, "비밀번호는 4자 이상이어야 합니다.", false);
            }
            
            // 회원 존재 확인
            Member member = memberMapper.getMemberInfo(id);
            if (member == null) {
                return new ServiceResult<>(false, "회원을 찾을 수 없습니다.", false);
            }
            
            // 비밀번호 암호화 후 업데이트
            String encodedPassword = passwordEncoder.encode(newPassword);
            int result = memberMapper.updatePassword(id, encodedPassword);
            
            if (result > 0) {
                return new ServiceResult<>(true, "비밀번호가 성공적으로 변경되었습니다.", true);
            } else {
                return new ServiceResult<>(false, "비밀번호 변경에 실패했습니다.", false);
            }
        } catch (Exception e) {
            return new ServiceResult<>(false, "비밀번호 재설정 중 오류가 발생했습니다: " + e.getMessage(), false);
        }
    }

    // --------------------------
    // 로그아웃 처리
    // --------------------------
    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }
}


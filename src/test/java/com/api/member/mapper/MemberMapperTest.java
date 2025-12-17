package com.api.member.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.api.member.domain.Member;

@SpringBootTest
@Transactional
class MemberMapperTest {

    @Autowired
    private MemberMapper memberMapper;

    @Test
    void testGetMemberInfo() {
        // 테스트: 존재하지 않는 회원 조회
        Member member = memberMapper.getMemberInfo("test123");
        assertNull(member, "존재하지 않는 회원은 null이어야 합니다.");
    }

    @Test
    void testInsertMember() {
        // 테스트: 회원 가입
        Member newMember = new Member();
        newMember.setId("testuser");
        newMember.setPass("$2a$10$testpasswordhash");
        newMember.setName("테스트 사용자");
        newMember.setPhone("01012345678");
        newMember.setMail("test@example.com");
        newMember.setZipcode(12345);
        newMember.setAddress1("서울시 강남구");
        newMember.setAddress2("테스트동 123");
        newMember.setMarketing("Y");
        newMember.setType("USER");

        int result = memberMapper.insertMember(newMember);
        assertEquals(1, result, "회원 가입은 1개의 행을 반환해야 합니다.");

        // 테스트: 가입한 회원 조회
        Member found = memberMapper.getMemberInfo("testuser");
        assertNotNull(found, "가입한 회원을 찾을 수 있어야 합니다.");
        assertEquals("테스트 사용자", found.getName(), "회원 이름이 일치해야 합니다.");
        assertEquals("test@example.com", found.getMail(), "이메일이 일치해야 합니다.");
    }

    @Test
    void testUpdateMember() {
        // 먼저 회원 가입
        Member member = new Member();
        member.setId("updateuser");
        member.setPass("$2a$10$testpasswordhash");
        member.setName("업데이트 테스트");
        member.setPhone("01011111111");
        member.setMail("update@example.com");
        member.setZipcode(54321);
        member.setAddress1("서울시 서초구");
        member.setAddress2("업데이트동 456");
        member.setMarketing("N");
        member.setType("USER");
        memberMapper.insertMember(member);

        // 회원 정보 수정
        member.setName("수정된 이름");
        member.setPhone("01099999999");
        member.setMail("updated@example.com");
        
        int result = memberMapper.updateMember(member);
        assertEquals(1, result, "회원 정보 수정은 1개의 행을 반환해야 합니다.");

        // 수정된 정보 확인
        Member updated = memberMapper.getMemberInfo("updateuser");
        assertNotNull(updated, "수정된 회원을 찾을 수 있어야 합니다.");
        assertEquals("수정된 이름", updated.getName(), "이름이 수정되어야 합니다.");
        assertEquals("01099999999", updated.getPhone(), "전화번호가 수정되어야 합니다.");
        assertEquals("updated@example.com", updated.getMail(), "이메일이 수정되어야 합니다.");
    }
}


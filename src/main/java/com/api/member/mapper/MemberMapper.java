package com.api.member.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.api.member.domain.Member;

@Mapper
public interface MemberMapper {

    Member getMemberInfo(String id);
    
    int insertMember(Member member);
    
    int updateMember(Member member);
    
    int deleteMember(String id);
    
    // 아이디 찾기: 이름과 전화번호로 회원 조회
    Member findMemberByNameAndPhone(String name, String phone);
    
    // 비밀번호 찾기: 아이디, 이름, 전화번호로 회원 조회
    Member findMemberByIdNameAndPhone(String id, String name, String phone);
    
    // 비밀번호 업데이트
    int updatePassword(String id, String encodedPassword);
}

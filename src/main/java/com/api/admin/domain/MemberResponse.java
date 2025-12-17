package com.api.admin.domain;

import com.api.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private String id;
    private String name;
    private String mail;
    private String phone;
    private String type;

    public static MemberResponse from(Member member) {
        if (member == null) {
            return null;
        }
        return MemberResponse.builder()
            .id(member.getId())
            .name(member.getName())
            .mail(member.getMail())
            .phone(member.getPhone())
            .type(member.getType())
            .build();
    }
}


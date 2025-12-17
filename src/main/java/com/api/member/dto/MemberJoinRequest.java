package com.api.member.dto;

import com.api.member.domain.Member;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberJoinRequest {
    private String id;
    private String pass1;
    private String pass2;
    private String name;
    private String mobile1;
    private String mobile2;
    private String zipcode;
    private String address1;
    private String address2;
    private String mail;
    private String marketing;

    public String getPass() {
        return pass1;
    }

    public String getPhone() {
        if (mobile1 == null || mobile2 == null) {
            return null;
        }
        return mobile1 + mobile2;
    }

    public int getZipcodeInt() {
        try {
            return zipcode != null && !zipcode.isEmpty() ? Integer.parseInt(zipcode) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Member toMember() {
        Member member = new Member();
        member.setId(this.id);
        member.setPass(this.pass1);
        member.setName(this.name);
        member.setPhone(getPhone());
        member.setMail(this.mail);
        member.setZipcode(getZipcodeInt());
        member.setAddress1(this.address1);
        member.setAddress2(this.address2);
        member.setMarketing(this.marketing);
        return member;
    }
}

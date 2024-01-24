package com.ssafy.seas.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
	private String pwd;
	private String name;
	private String email;
	private Integer point = 0;
}

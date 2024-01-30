package com.ssafy.seas.member.dto;

import com.querydsl.core.annotations.QueryProjection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberDto {
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Post {
		private String memberId;
		private String password;
		private String nickname;
		private String email;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Response {
		private String memberId;
		private String password;
		private String nickname;
		private String email;
		private Integer point;
	}

	@NoArgsConstructor
	@Getter
	public static class MyInfoResponse {
		private String nickname;
		private Integer point;
		private String tier;
		private Integer solvedCount = 0;
		// 소수점 아래 1번째 자리까지 표시
		private Double correctRate = 0.0;

		@QueryProjection
		public MyInfoResponse(String nickname, Integer point, String tier) {
			this.nickname = nickname;
			this.point = point;
			this.tier = tier;
		}

		@QueryProjection
		public MyInfoResponse(Integer solvedCount, Double correctRate) {
			this.solvedCount = solvedCount;
			this.correctRate = Math.round(correctRate*10)/10.0;
		}

		@Builder
		public MyInfoResponse(String nickname, Integer point, String tier, Integer solvedCount, Double correctRate) {
			this.nickname = nickname;
			this.point = point;
			this.tier = tier;
			this.solvedCount = solvedCount;
			this.correctRate = Math.round(correctRate*10)/10.0;
		}
	}
}

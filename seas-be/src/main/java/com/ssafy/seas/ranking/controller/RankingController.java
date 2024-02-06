package com.ssafy.seas.ranking.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.seas.common.constants.ErrorCode;
import com.ssafy.seas.common.constants.SuccessCode;
import com.ssafy.seas.common.dto.ApiResponse;
import com.ssafy.seas.ranking.dto.BadgeDto;
import com.ssafy.seas.ranking.dto.RankDto;
import com.ssafy.seas.ranking.dto.RankerDto;
import com.ssafy.seas.ranking.service.RankingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ranking")
public class RankingController {
	private final RankingService rankingService;

	@GetMapping("/list")
	public ApiResponse<RankDto.Response> getRankingList() {
		try {
			String uuid = "toast";
			List<RankerDto.RankResponse> rankerDtoList = rankingService.getRankers();
			List<RankerDto.RankResponse> myRankDto = rankingService.getMyRank(uuid);

			for(RankerDto.RankResponse currentRanker : rankerDtoList){
				List<BadgeDto.BadgeResponse> badgeList = rankingService.getBadgeList(currentRanker.getNickname());
				currentRanker.setBadgeList(badgeList);
			}

			if(myRankDto.size() > 0){
				RankerDto.RankResponse myDto = myRankDto.get(0);
				myDto.setBadgeList(rankingService.getBadgeList(myDto.getNickname()));
			}

			return ApiResponse.success(SuccessCode.GET_SUCCESS, new RankDto.Response(rankerDtoList, myRankDto.get(0)));
		} catch (Exception e) {
			e.printStackTrace();
			return ApiResponse.error(ErrorCode.SERVER_ERROR);
		}
	}

	@GetMapping
	public ApiResponse<RankerDto.RankResponseWithRanking> getMemberRanking(@RequestParam("search") String searchNickname) {
		try {
			List<RankerDto.RankResponseWithRanking> result = rankingService.getRankByNickname(searchNickname);
			return ApiResponse.success(SuccessCode.GET_SUCCESS, result.get(0));
		} catch (Exception e) {
			e.printStackTrace();
			return ApiResponse.error(ErrorCode.SERVER_ERROR);
		}
	}
}

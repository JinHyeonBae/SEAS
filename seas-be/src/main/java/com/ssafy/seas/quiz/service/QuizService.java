package com.ssafy.seas.quiz.service;


import com.ssafy.seas.member.util.MemberUtil;
import com.ssafy.seas.quiz.dto.*;
import com.ssafy.seas.quiz.repository.CorrectAnswerRepository;
import com.ssafy.seas.quiz.repository.FactorRepository;
import com.ssafy.seas.quiz.repository.QuizCustomRepository;
import com.ssafy.seas.quiz.repository.WrongAnswerRepostory;
import com.ssafy.seas.quiz.util.QuizUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizService {

    private final QuizCustomRepository quizCustomRepository;
    private final FactorRepository factorRepository;
    private final WrongAnswerRepostory wrongAnswerRepostory;
    private final CorrectAnswerRepository correctAnswerRepository;
    private final QuizUtil quizUtil;
    private final MemberUtil memberUtil;

    /*
        현재 문제 :
            swagger에서  가 들어갈 때 JSON 에러 남
     */


    public QuizListDto.Response getQuizzes(Integer categoryId){

        Integer memberId = memberUtil.getLoginMemberId();

        List<QuizListDto.QuizInfo> quizInfoList = new ArrayList<>();

        // factor 테이블에 아직 없을 시에는 interval, ef null
        List<QuizDto.QuizFactorDto> quizFactors = quizCustomRepository.findAllQuizInnerJoin(memberId, categoryId);
        List<QuizDto.QuizWeightInfoDto> quizWeightInfos = new ArrayList<>();

        // 아무런 문제도 안 풀었을 경우(=처음) or, 10개 미만으로 풀었을 경우, 모든 문제를 불러옴
//        if(quizFactors.size() < 10){
//            int requiredCount = 10 - quizFactors.size();
//            log.info("불러와야 하는 문제 수 : " +requiredCount);
//
//            List<QuizDto.QuizInfoDto> quizInfos = quizCustomRepository.findQuizzesLimitedBy(requiredCount, categoryId);
//            log.info(">>>>>> QUIZINFO SIZE : " + quizInfos.size());
//            List<QuizDto.QuizFactorDto> infos = quizInfos.stream().map(dto -> new QuizDto.QuizFactorDto(memberId, dto.getQuizId(), dto.getQuiz(), dto.getHint(), Interval.FIRST.getValue(), EasinessFactor.MINIMUM.getValue())).collect(Collectors.toList());
//
//            quizFactors.addAll(infos);
//        }

        // factor 테이블의 정보를 저장
        quizWeightInfos.addAll(
                quizFactors.stream().map(dto -> {
                    return new QuizDto.QuizWeightInfoDto(dto.getQuizId(), dto.getQuizInterval(), dto.getEf());
                }).collect(Collectors.toList())
        );

        for(int i = 0; i < 10; i++) {
            double[][] prefixWeightList = quizUtil.getPrefixWeightArray(quizWeightInfos);
            double[] selectedQuizInfo = quizUtil.selectQuizzes(prefixWeightList);
            int foundIndex = (int) selectedQuizInfo[2];
            quizWeightInfos.remove(foundIndex);

            int quizId = (int) selectedQuizInfo[0];
            String quiz = quizFactors.stream().filter(dto -> dto.getQuizId() == quizId).findFirst().get().getQuiz();
            quizInfoList.add(new QuizListDto.QuizInfo(quizId, quiz));
        }

        quizUtil.storeQuizToRedis(memberId, quizFactors);

        return new QuizListDto.Response(quizInfoList);
    }

    public QuizHintDto.Response getHint(Integer quizId){

        Integer memberId = memberUtil.getLoginMemberId();

        quizUtil.updateHintState(memberId, quizId);
        String hint = quizUtil.getQuizHint(memberId, quizId);
        return new QuizHintDto.Response(quizId, hint);
    }


    @Transactional
    public QuizAnswerDto.Response getSubmitResult(QuizAnswerDto.Request request, Integer categoryId, Integer quizId) throws ServerErrorException {

        String submit = request.getSubmit().replaceAll("\s+", "").replaceAll("\t+", "").replaceAll(" ", "").toLowerCase().trim();

        List<String> quizAnswers = quizCustomRepository.findAllQuizAnswerByQuizId(quizId);

        Integer memberId = memberUtil.getLoginMemberId();

        for(String quizAnswer : quizAnswers){
            if(quizAnswer.equals(submit)){
                // 퀴즈 정답 횟수 + 1
                // 카테고리 별 맞힌 횟수 + 1

                quizUtil.updateQuizAnswerState(memberId, quizId);
                QuizAnswerDto.UpdatedFactors factor = quizUtil.getNewFactor(memberId, quizId, categoryId);
                // factor 갱신


                correctAnswerRepository.saveOrUpdateStreakAndScoreHistory(factor);
                correctAnswerRepository.saveOrUpdateFactor(factor, memberId);
                return new QuizAnswerDto.Response(true);
            }
        }

        QuizAnswerDto.UpdatedFactors factor = quizUtil.getNewFactor(memberId, quizId, categoryId);
        wrongAnswerRepostory.saveOrUpdateIncorrectNoteAndSolvedQuiz(memberId, quizId);
        wrongAnswerRepostory.saveOrUpdateFactor(memberId, factor);
        return new QuizAnswerDto.Response(false);
    }


    public QuizResultDto.Response getTotalResult(){

        Integer memberId = memberUtil.getLoginMemberId();

        QuizResultDto.Response response = quizUtil.getResult(memberId);
        quizUtil.resetRedis(memberId);
        return response;
    }

    public QuizDto.BaseResponse redisReset(){
        Integer memberId = memberUtil.getLoginMemberId();
        quizUtil.resetRedis(memberId);

        return new QuizDto.BaseResponse("레디스 값 삭제 성공");
    }

    public QuizTierDto.Response getCurrentTier(){
        return new QuizTierDto.Response("선원", false);
    }

    public QuizTierDto.Response getTier(String prevTier){

        QuizTierDto.Response response = new QuizTierDto.Response("선장",true);

        return new QuizTierDto.Response("선장",true);
    }
}

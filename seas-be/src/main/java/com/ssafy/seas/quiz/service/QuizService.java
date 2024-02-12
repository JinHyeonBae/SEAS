package com.ssafy.seas.quiz.service;


import com.ssafy.seas.member.util.MemberUtil;
import com.ssafy.seas.quiz.constant.EasinessFactor;
import com.ssafy.seas.quiz.constant.Interval;
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
            틀렸을 때 factor 테이블 갱신 X
            swagger에서  가 들어갈 때 JSON 에러 남
     */

    /**
     * 로직 설명:
     *
     * factor 테이블은 사용자가 퀴즈를 풀다가 나가도 저장이 된다.
     * 그러므로, 2문제를 푼 경우 2문제에 대한 ef, factor가 있을 것이므로 해당 정보가 필요하고, 나머지 문제들은 모두 불러와야 한다.
     * 일단 factor에서 모두 가져오고, 만일 10개가 충족이 안되면 모든 문제를 들고와서 quizWeightInfos에 넣는다.
     * 이후 factor에 있는 모든 정보를 quizWeightInfos에 저장한다.
     *
     * 레디스는 Map으로 저장되고 있으므로, 만일 같은 키가 들어왔으면 이후의 값으로 대체되므로, 결국 factor의 테이블에 있는 정보가 레디스에 저장된다.
     */

    public QuizListDto.Response getQuizzes(Integer categoryId){

        Integer memberId = MemberUtil.getLoginMemberId();

        List<QuizListDto.QuizInfo> quizInfoList = new ArrayList<>();

        // factor 테이블에 아직 없을 시에는 interval, ef null
        List<QuizDto.QuizFactorDto> quizFactors = quizCustomRepository.findAllQuizInnerJoin(memberId, categoryId);
        List<QuizDto.QuizWeightInfoDto> quizWeightInfos = new ArrayList<>();

        // 아무런 문제도 안 풀었을 경우(=처음) or, 10개 미만으로 풀었을 경우, 모든 문제를 불러옴
        if(quizFactors.size() < 10){
            int requiredCount = 10 - quizFactors.size();
            log.info("불러와야 하는 문제 수 : " +requiredCount);

            List<QuizDto.QuizInfoDto> quizInfos = quizCustomRepository.findQuizzesLimitedBy(requiredCount, categoryId);
            log.info(">>>>>> QUIZINFO SIZE : " + quizInfos.size());
            List<QuizDto.QuizFactorDto> infos = quizInfos.stream().map(dto -> new QuizDto.QuizFactorDto(memberId, dto.getQuizId(), dto.getQuiz(), dto.getHint(), Interval.FIRST.getValue(), EasinessFactor.MINIMUM.getValue())).collect(Collectors.toList());

            quizFactors.addAll(infos);
        }

        // factor 테이블의 정보를 저장
        quizWeightInfos.addAll(
                quizFactors.stream().map(dto -> {
                    return new QuizDto.QuizWeightInfoDto(dto.getQuizId(), dto.getQuizInterval(), dto.getEf());
                }).collect(Collectors.toList())
        );

        log.info(">>>>>>>>>> QUIZ_WEIGHT_INFO : " + quizWeightInfos.size());

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

        Integer memberId = MemberUtil.getLoginMemberId();

        quizUtil.updateHintState(memberId, quizId);
        String hint = quizUtil.getQuizHint(memberId, quizId);
        return new QuizHintDto.Response(quizId, hint);
    }


    @Transactional
    public QuizAnswerDto.Response getSubmitResult(QuizAnswerDto.Request request, Integer categoryId, Integer quizId) throws ServerErrorException {

        String submit = request.getSubmit().replaceAll("\s+", "").replaceAll("\t+", "").replaceAll(" ", "").toLowerCase().trim();

        List<String> quizAnswers = quizCustomRepository.findAllQuizAnswerByQuizId(quizId);

        Integer memberId = MemberUtil.getLoginMemberId();

        for(String quizAnswer : quizAnswers){
            if(quizAnswer.equals(submit)){
                // 퀴즈 정답 횟수 + 1
                // 카테고리 별 맞힌 횟수 + 1

                quizUtil.updateQuizState(memberId, quizId);
                QuizAnswerDto.UpdatedFactors factor = quizUtil.getNewFactor(memberId, quizId, categoryId);
                // factor 갱신
                factorRepository.updateFactor(factor.getEf(), factor.getInterval(), quizId, memberId);
                //correctAnswerRepository.
                correctAnswerRepository.saveOrUpdateStreakAndScoreHistory(factor);
                correctAnswerRepository.saveOrUpdateFactorAndSolvedQuiz(factor);
                return new QuizAnswerDto.Response(true);
            }
        }

        QuizAnswerDto.UpdatedFactors factor = quizUtil.getNewFactor(memberId, quizId, categoryId);
        wrongAnswerRepostory.saveOrUpdateIncorrectNoteAndSolvedQuiz(memberId, quizId);
        //factor 테이블 갱신
        factorRepository.updateFactor(factor.getEf(), factor.getInterval(), quizId, memberId);
        return new QuizAnswerDto.Response(false);
    }


    public QuizResultDto.Response getTotalResult(){

        Integer memberId = MemberUtil.getLoginMemberId();

        QuizResultDto.Response response = quizUtil.getResult(memberId);
        quizUtil.resetRedis(memberId);
        return response;
    }

    public QuizTierDto.Response getCurrentTier(){
        return new QuizTierDto.Response("선원", false);
    }

    public QuizTierDto.Response getTier(String prevTier){

        QuizTierDto.Response response = new QuizTierDto.Response("선장",true);

        return new QuizTierDto.Response("선장",true);
    }
}

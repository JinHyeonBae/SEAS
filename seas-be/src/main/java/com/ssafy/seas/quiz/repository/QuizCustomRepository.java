package com.ssafy.seas.quiz.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.seas.quiz.dto.QQuizDto_QuizFactorDto;
import com.ssafy.seas.quiz.dto.QQuizDto_QuizInfoDto;
import com.ssafy.seas.quiz.dto.QuizDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.ssafy.seas.quiz.entity.QCardQuiz.cardQuiz;
import static com.ssafy.seas.quiz.entity.QFactor.factor;
import static com.ssafy.seas.quiz.entity.QQuiz.quiz;
import static com.ssafy.seas.quiz.entity.QQuizAnswer.quizAnswer;


@Repository
@Slf4j
@RequiredArgsConstructor
public class QuizCustomRepository{

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    // 멤버별 factor 테이블의 퀴즈 정보를 가져옴
    // 불변 리스트인가봐 queryDsl
    public List<QuizDto.QuizFactorDto> findAllQuizInnerJoin(Integer memberId, Integer categoryId) {

        log.info(memberId + "||" + categoryId);
        List<QuizDto.QuizFactorDto> QuizInfoList = //new ArrayList<>();
                jpaQueryFactory
                .select(new QQuizDto_QuizFactorDto(factor.member.id, quiz.id, quiz.problem, quiz.hint, factor.quizInterval, factor.ef))
                .from(factor)
                .join(factor.cardQuiz).join(cardQuiz.quiz)
                .on(factor.cardQuiz.id.eq(cardQuiz.id))
                        .on(cardQuiz.quiz.id.eq(quiz.id))
                        //.fetchJoin()
                .where(factor.member.id.eq(memberId.intValue()),quiz.category.id.eq(categoryId.intValue()))
                        .fetch();



        log.info(QuizInfoList.toString());

        return QuizInfoList;
    }

    public List<QuizDto.QuizInfoDto> findQuizzesLimitedBy(Integer requiredCount, Integer categoryId){
        return
                jpaQueryFactory
                .select(new QQuizDto_QuizInfoDto(quiz.id, quiz.problem, quiz.hint))
                .from(quiz)
                .where(quiz.category.id.eq(categoryId.intValue()))
                .fetch();
    }


    public List<String> findAllQuizAnswerByQuizId(Integer quizId){

        return jpaQueryFactory
                            .select(quizAnswer.answer)
                .from(quiz)
                .leftJoin(quizAnswer)
                .on(quiz.id.eq(quizAnswer.quiz.id))
                .where(quiz.id.eq(quizId))
                .stream()
                .map(ans -> ans.replaceAll("\s+", "").replaceAll("\t+", "").replaceAll(" ", "").toLowerCase().trim())
                .collect(Collectors.toList());

    }

}

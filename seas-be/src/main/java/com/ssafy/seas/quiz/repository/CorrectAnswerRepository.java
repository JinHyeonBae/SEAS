package com.ssafy.seas.quiz.repository;

import com.ssafy.seas.category.entity.Category;
import com.ssafy.seas.member.entity.Member;
import com.ssafy.seas.mypage.entity.Streak;
import com.ssafy.seas.quiz.dto.QuizAnswerDto;
import com.ssafy.seas.quiz.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ServerErrorException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

@Repository
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CorrectAnswerRepository {

    private final EntityManager entityManager;

    public void saveOrUpdateStreakAndScoreHistory(QuizAnswerDto.UpdatedFactors factors) {
        // Member와 Streak, ScoreHistory를 조인하여 값을 조회, 날짜 기준 오늘
        LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        String jpql = "SELECT m, s FROM Member m " +
                "LEFT JOIN Streak s ON m.id = s.member.id AND s.updatedAt BETWEEN :startDateTime AND :endDateTime " +
                //"LEFT JOIN ScoreHistory sh ON m.id = sh.member.id AND sh.category.id = :categoryId " +
                "WHERE m.id = :memberId";

        Member member = entityManager.getReference(Member.class, factors.getMemberId());

        Query query = entityManager.createQuery(jpql, Object[].class);
        query.setParameter("memberId", factors.getMemberId());
        query.setParameter("startDateTime", startDateTime);
        query.setParameter("endDateTime", endDateTime);

        Object[] result = (Object[]) query.getSingleResult();

        log.info("RESULT : " + Arrays.stream(result).count());
        log.info("RESULT : " + Arrays.stream(result).findFirst());
        Member existingMember = (Member) result[0];
        Streak existingStreak = (Streak) result[1];

        if (existingStreak == null) {
            // Member나 Streak가 없는 경우에는 새로운 엔티티 생성
            Streak newStreak = new Streak(member, 1);
            entityManager.persist(newStreak);
        } else {
            // Member와 Streak가 있는 경우, 값을 업데이트
            Integer quizCount = existingStreak.getQuizCount();
            log.info(existingStreak.toString());
            existingStreak.setQuizCount(quizCount + 1);
        }

        // scoreHistory insert
        Category category = entityManager.getReference(Category.class, factors.getCategoryId());

        ScoreHistory newScoreHistory = new ScoreHistory(member, category, factors.getScore());
        entityManager.persist(newScoreHistory);

        // FIX : solveQuiz 검사 후 해당 날짜에 맞은 흔적이 없으면 갱신
        Integer point = existingMember.getPoint();
        existingMember.setPoint(point + factors.getPoint());
    }


    public void
    saveOrUpdateFactorAndSolvedQuiz(QuizAnswerDto.UpdatedFactors factors) throws ServerErrorException{
            // Factor와 CardQuiz를 조인하여 값을 조회
            String jpql = "SELECT f, cq FROM Factor f " +
                    "JOIN f.cardQuiz cq " +
                    "WHERE f.member.id = :memberId AND cq.quiz.id = :quizId";

            LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

            Query factorQuery = entityManager.createQuery(jpql, Object[].class);
            factorQuery.setParameter("memberId", factors.getMemberId());
            factorQuery.setParameter("quizId", factors.getQuizId());

            if(factorQuery.getResultList().isEmpty()) {
                Member member = entityManager.getReference(Member.class, factors.getMemberId());
                Quiz quiz = entityManager.getReference(Quiz.class, factors.getQuizId());

                CardQuiz newCardQuiz = new CardQuiz();
                newCardQuiz.setQuiz(quiz);

                entityManager.persist(newCardQuiz);

                Factor newFactor = new Factor(member, newCardQuiz, factors.getInterval(), factors.getEf());

                entityManager.persist(newFactor);
            }
            else {
                Object[] result = (Object[]) factorQuery.getSingleResult();

                Factor factor = (Factor) result[0];
                CardQuiz cardQuiz = (CardQuiz) result[1];
                // Factor와 CardQuiz가 있는 경우, 값을 업데이트
                factor.updateFactor(factors.getInterval(), factors.getEf());
    //                // SolvedQuiz 엔티티 업데이트
    //                solvedQuiz.updateCount(true);
            }

            jpql = "SELECT sq FROM SolvedQuiz sq " +
                    "WHERE sq.quiz.id = :quizId " +
                    "AND sq.updatedAt BETWEEN :startDateTime AND :endDateTime";

            Query solveQuizQuery = entityManager.createQuery(jpql, Object[].class);
            solveQuizQuery.setParameter("quizId", factors.getQuizId());
            solveQuizQuery.setParameter("startDateTime", startDateTime);
            solveQuizQuery.setParameter("endDateTime", endDateTime);

            if(solveQuizQuery.getResultList().isEmpty()){
                Member member = entityManager.getReference(Member.class, factors.getMemberId());
                Quiz quiz = entityManager.getReference(Quiz.class, factors.getQuizId());

                SolvedQuiz solvedQuiz = new SolvedQuiz(member, quiz);
                solvedQuiz.updateCount(true);
                entityManager.persist(solvedQuiz);
            }
            else{
                Object[] solveQuizResult = (Object[]) solveQuizQuery.getSingleResult();
                SolvedQuiz solvedQuiz = (SolvedQuiz) solveQuizResult[0];

                // 오늘 풀지 않았거나, 틀림
                if(solvedQuiz.getCorrectCount() < 1) {
                    solvedQuiz.updateCount(true);
                    entityManager.merge(solvedQuiz);
                }
            }
    }
}


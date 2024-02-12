package com.ssafy.seas.quiz.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

public class QuizDto {

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class QuizFactorDto implements Serializable{
        private Double quizInterval;
        private Double ef;

        private Integer quizId;
        private String quiz;
        private String hint;

        private Integer memberId;

        private Boolean isCorrect = false;
        private Boolean isUsedHint = false;

        @QueryProjection
        public QuizFactorDto(Integer memberId, Integer quizId, String quiz, String hint, Double quizInterval, Double ef){
            this.memberId = memberId;
            this.quizId = quizId;
            this.quiz = quiz;
            this.hint = hint;
            this.quizInterval = quizInterval;
            this.ef = ef;
            this.isCorrect = false;
            this.isUsedHint = false;
        }

        public void setIsCorrect(Boolean result){
            this.isCorrect = result;
        }

        public void setUsedHint(Boolean isUsed){
            this.isUsedHint = isUsed;
        }

        @Override
        public String toString() {
            return "QuizFactorDto{" +
                    "quizInterval=" + quizInterval +
                    ", ef=" + ef +
                    ", quizId=" + quizId +
                    ", quiz='" + quiz + '\'' +
                    ", hint='" + hint + '\'' +
                    ", isCorrect=" + isCorrect +
                    ", isUsedHint=" + isUsedHint +
                    '}';
        }
    }

    @Getter
    @Setter
    public static class QuizWeightInfoDto{
        private final Integer quizId;
        private final Double quizInterval;
        private final Double ef;

        @QueryProjection
        public QuizWeightInfoDto(Integer quizId, Double quizInterval, Double ef){
            this.quizId = quizId;
            this.quizInterval = quizInterval;
            this.ef = ef;
        }
    }

    @Getter
    public static class QuizInfoDto{
        private Integer quizId;
        private String quiz;
        private String hint;

        @QueryProjection
        public QuizInfoDto(Integer quizId, String quiz, String hint){
            this.quizId = quizId;
            this.quiz = quiz;
            this.hint = hint;
        }
    }




}

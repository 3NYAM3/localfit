package com.localfit.domain.user.entity;

/**
 * 사용자가 각 지표에 매기는 중요도 등급.
 * 숫자를 직접 입력 받는 대신 직관적인 선택지를 주고, 서버가 이값을 비율로 환산해 가중치를 계산
 *
 * NOT_IMPORTANT는 가중치가 0이라 모든 지표가 NOT_IMPORTANT일 경우 모든 지표가 동일가중치인것으로 처리
 */
public enum ImportanceLevel {
    VERY_IMPORTANT(4),
    IMPORTANT(3),
    NORMAL(2),
    NOT_IMPORTANT(0);

    private final int weight;

    ImportanceLevel(int weight) {
        this.weight = weight;
    }
    public int getWeight(){
        return weight;
    }

}

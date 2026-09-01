package com.localfit.domain.region.entity;

import com.localfit.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지역(법정동 단위) 마스터 데이터.
 *
 * [중요] 이 Entity는 특정 사용자의 데이터가 아니라, 시스템이 미리 채워두는 "기준 데이터"다.
 * 대한민국(현재는 수도권)에 존재하는 법정동 목록을 한 번 수집해서 저장해두고,
 * 앞으로 만들 다른 도메인(전월세, 버스정류장, 병원, 학교 등)이 이 Region을
 * 외래키로 참조하는 구조가 된다. 즉 모든 사용자가 이 데이터를 공유한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "region")
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String regionCode;

    @Column(nullable = false, length = 20)
    private String sido;

    @Column(nullable = false, length = 20)
    private String sigungu;

    @Column(length = 20)
    private String dong;

    /**
     * 생성자는 @Builder로만 열어둔다.
     * Setter를 두지 않는 이유: Entity를 아무 곳에서나 임의로 수정 가능하게 열어두면
     * 나중에 데이터 일관성 문제를 추적하기 어려워지기 때문.
     */
    @Builder
    public Region(String regionCode, String sido, String sigungu, String dong) {
        this.regionCode = regionCode;
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
    }

}

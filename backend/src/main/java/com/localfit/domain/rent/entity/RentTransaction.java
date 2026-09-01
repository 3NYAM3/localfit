package com.localfit.domain.rent.entity;

import com.localfit.domain.region.entity.Region;
import com.localfit.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 아파트 전월세 실거래 1건.
 *
 * Region을 @ManyToOne으로 참조한다
 * fetch = LAZY: 실거래 조회 시 Region 정보가 항상 필요한 건 아니므로,
 * 실제로 접근할 때만 조회하도록 지연 로딩으로 설정.
 */

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rent_transaction")
public class RentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 100)
    private String aptName;

    @Column(nullable = false)
    private Double excluUseArea;

    @Column(nullable = false)
    private LocalDate dealDate;

    @Column(nullable = false)
    private Long deposit;

    @Column(nullable = false)
    private Long monthlyRent;

    private Integer floor;
    private Integer buildYear;

    @Builder
    public RentTransaction(Region region, String aptName, Double excluUseArea,
                           LocalDate dealDate, Long deposit, Long monthlyRent,
                           Integer floor, Integer buildYear) {
        this.region = region;
        this.aptName = aptName;
        this.excluUseArea = excluUseArea;
        this.dealDate = dealDate;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.floor = floor;
        this.buildYear = buildYear;
    }

}

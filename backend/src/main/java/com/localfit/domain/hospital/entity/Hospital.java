package com.localfit.domain.hospital.entity;

import com.localfit.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 병원 정보
 * 상급병원, 종합병원, 병원 급만 수집 다른 의원들까지 수집하면 지표를 왜곡할 수 있어 제외
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "hospital")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ykiho;   // 암호화된 요양기호(고유식별자)

    @Column(nullable = false, length = 100)
    private String hospitalName;

    @Column(nullable = false, length = 20)
    private String clCode;      //종별코드 (01/11/21)

    @Column(nullable = false, length = 20)
    private String clCodeName;  // 종별코드명 (상급병원/종합병원/병원)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private Double latitude;
    private Double longitude;

    @Builder
    public Hospital(String ykiho, String hospitalName, String clCode, String clCodeName,  Region region, Double latitude, Double longitude){
        this.ykiho = ykiho;
        this.hospitalName = hospitalName;
        this.clCode =clCode;
        this.clCodeName = clCodeName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

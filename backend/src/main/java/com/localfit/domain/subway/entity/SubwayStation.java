package com.localfit.domain.subway.entity;

import com.localfit.domain.region.entity.Region;
import com.localfit.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subway_station")
public class SubwayStation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String stationCode;

    @Column(nullable = false, length = 50)
    private String stationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private Double latitude;
    private Double longitude;

    @Builder
    public SubwayStation(String stationCode, String stationName, Region region,
                         Double latitude, Double longitude) {
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

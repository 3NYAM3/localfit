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
@Table(
        name = "subway_station",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_station_name_region",
                columnNames = {"normalized_name", "region_id"}
        )
)
public class SubwayStation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String stationName;         // 원본 역명

    @Column(nullable = false, length = 50)
    private String normalizedName;      // 정규화 역명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private Double latitude;
    private Double longitude;

    @Builder
    public SubwayStation(String stationName, String normalizedName, Region region,
                         Double latitude, Double longitude) {
        this.stationName = stationName;
        this.normalizedName = normalizedName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

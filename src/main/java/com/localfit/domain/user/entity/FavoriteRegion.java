package com.localfit.domain.user.entity;

import com.localfit.domain.region.entity.Region;
import com.localfit.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "favorite_region",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "region_id"})
)
public class FavoriteRegion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Builder
    public FavoriteRegion(User user, Region region){
        this.user = user;
        this.region = region;
    }
}

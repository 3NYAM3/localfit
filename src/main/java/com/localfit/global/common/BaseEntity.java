package com.localfit.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 Entity가 상속받는 공통 베이스 클래스.
 * 생성/수정 시각을 각 Entity마다 반복 작성하지 않도록 여기서 한 번만 정의한다.
 *
 * @MappedSuperclass - 이 클래스 자체는 테이블로 생성되지 않고, 상속한 Entity의 테이블에 필드만 포함된다.
 * @EntityListeners - 저장/수정 시점에 createdAt, updatedAt을 자동으로 채워주는 리스너 등록.
 */

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}

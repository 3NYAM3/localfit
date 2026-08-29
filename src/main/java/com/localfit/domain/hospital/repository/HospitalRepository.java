package com.localfit.domain.hospital.repository;

import com.localfit.domain.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /** ykiho로 중복 여부 확인 */
    boolean existsByYkiho(String ykiho);
}

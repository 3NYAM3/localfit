package com.localfit.domain.hospital.repository;

import com.localfit.domain.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    boolean existsByYkiho(String ykiho);
}

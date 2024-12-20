package com.scv.domain.result.repository;

import com.scv.domain.result.domain.Result;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Result r WHERE r.id = :id")
    Optional<Result> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Result r SET r.deleted = true WHERE r.modelVersion.id = :modelVersionId")
    void softDeleteByModelVersionId(@Param("modelVersionId") Long modelVersionId);

    Optional<Result> findByIdAndDeletedFalse(Long modelVersionId);
}

package com.scv.domain.data.repository;

import com.scv.domain.data.domain.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataRepository extends JpaRepository<Data, Integer> {

    Optional<Data> findByName(String dataName);
}

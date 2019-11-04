package org.ssu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssu.entity.ChairEntity;

@Repository
public interface ChairRepository extends JpaRepository<ChairEntity, Integer> {
}
package org.ssu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssu.entity.Metadatavalue;


//@Repository
public interface MetadatavalueRepository extends JpaRepository<Metadatavalue, Integer> {

}
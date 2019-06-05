package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import ua.edu.sumdu.essuir.entity.Metadatavalue;

@Repository
public interface MetadatavalueRepository extends JpaRepository<Metadatavalue, Integer> {

}

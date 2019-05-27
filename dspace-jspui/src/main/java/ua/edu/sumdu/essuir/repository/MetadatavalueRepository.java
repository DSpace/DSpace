package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.essuir.entity.Metadatavalue;

import java.util.List;

@Repository
public interface MetadatavalueRepository extends JpaRepository<Metadatavalue, Integer> {

    List<Metadatavalue> findDistinctByTextValueContaining(String speciality);

    @Query("select distinct(resourceId) from Metadatavalue where textValue = 'Bachelous paper'")
    List<Integer> selectBachelousWorkIds();

    List<Metadatavalue> findDistinctByTextValue(String textValue);
    List<Metadatavalue> findByResourceIdIn(List<Integer> resourceIds);
}

package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.essuir.entity.Item;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    @Query("SELECT it " +
            "FROM Metadatavalue mv1 " +
            "LEFT JOIN FETCH Item it ON mv1.resourceId = it.itemId " +
            "WHERE it.inArchive = true AND (mv1.textValue = 'Bachelous paper' OR mv1.textValue = 'Masters thesis')")
    List<Item> selectBachelousAndMastersPapersWithMetadataFields();
}

package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.essuir.entity.AuthorLocalization;
import ua.edu.sumdu.essuir.entity.AuthorsLocalizationPK;

import java.util.List;

@Repository
public interface AuthorsRepository extends JpaRepository<AuthorLocalization, AuthorsLocalizationPK>{
    @Query("SELECT a FROM AuthorLocalization a WHERE a.authorsLocalizationPK.surname_en LIKE CONCAT(:letter, '%') ORDER BY a.authorsLocalizationPK.surname_en")
    List<AuthorLocalization> findAuthorsStartedByLetter(@Param("letter")String letter);
}

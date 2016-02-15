package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.essuir.entity.FacultyEntity;

@Repository
public interface FacultyRepository extends JpaRepository<FacultyEntity, Integer> {
}

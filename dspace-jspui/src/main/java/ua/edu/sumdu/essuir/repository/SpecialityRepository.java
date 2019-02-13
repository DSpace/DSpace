package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.essuir.entity.Speciality;

@Repository
public interface SpecialityRepository extends JpaRepository<Speciality, Integer>{
    Speciality findByName(String name);
    Speciality findByCode(String code);
}

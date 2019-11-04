package org.ssu.service;

import org.springframework.stereotype.Service;
import org.ssu.entity.FacultyEntity;
import org.ssu.repository.FacultyRepository;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FacultyService {
    @Resource
    private FacultyRepository facultyRepository;

    public List<FacultyEntity> getFacultyList() {
        return facultyRepository.findAll();
    }
}

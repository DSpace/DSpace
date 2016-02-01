package ua.edu.sumdu.essuir.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.entity.*;
import ua.edu.sumdu.essuir.repository.ChairRepository;
import ua.edu.sumdu.essuir.repository.FacultyRepository;
import ua.edu.sumdu.essuir.repository.PersonRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private ChairRepository chairRepository;

    @Autowired
    private PersonRepository personRepository;

    public List<FacultyEntity> getFacultyList() {
        return facultyRepository.findAll();
    }

    public List<ChairEntity> getChairList() {
        return chairRepository.findAll();
    }

    public List<Person> getPersonList() {
        List<Person> result = new ArrayList<>();
        HashMap<String, List<Submission>> submissions = new SubmissionHelper().getSubmissionList();
        for (EPerson person : personRepository.findAll()) {
            result.add(new Person(person, submissions.get(person.getEmail())));
        }
        return result;
    }
}

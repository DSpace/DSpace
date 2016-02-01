package ua.edu.sumdu.essuir.service;

import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.entity.EPerson;
import ua.edu.sumdu.essuir.entity.Person;
import ua.edu.sumdu.essuir.entity.Submission;
import ua.edu.sumdu.essuir.repository.PersonRepository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class ReportService {
    @Resource
    private PersonRepository personRepository;

    public List<Person> getPersonList() {
        List<Person> result = new LinkedList<>();
        HashMap<String, List<Submission>> submissions = new SubmissionHelper().getSubmissionList();
        for (EPerson person : personRepository.findAll()) {
            result.add(new Person(person, submissions.get(person.getEmail())));
        }
        return result;
    }
}

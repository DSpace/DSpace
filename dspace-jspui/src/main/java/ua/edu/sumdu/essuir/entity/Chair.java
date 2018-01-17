package ua.edu.sumdu.essuir.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

public class Chair {
    @JsonProperty("name")
    private String chairName;
    @JsonProperty("data")
    private List<Person> staff;

    public Chair(String chairName) {
        this.chairName = chairName;
        staff = new LinkedList<>();
    }

    @JsonProperty("submission_count")
    public Integer getSubmissionCount() {
        Integer result = 0;
        for (Person person : staff) {
            result += person.getSubmissionCount();
        }
        return result;
    }

    public void addSubmission(String personName, Integer submissionCount) {
        staff.add(new Person(personName, submissionCount));
    }

}

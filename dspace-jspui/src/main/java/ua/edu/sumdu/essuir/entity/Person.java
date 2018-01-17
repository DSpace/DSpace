package ua.edu.sumdu.essuir.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Person {
    @JsonProperty("name")
    private String name;
    @JsonProperty("submission_count")
    private Integer submissionCount;

    public Person(String name, Integer submissionCount) {
        this.name = name;
        this.submissionCount = submissionCount;
    }

    public Integer getSubmissionCount() {
        return submissionCount;
    }
}

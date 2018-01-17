package ua.edu.sumdu.essuir.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Faculty {
    @JsonProperty("name")
    private String facultyName;
    @JsonIgnore
    private HashMap<String, Chair> chairs;

    public Faculty(String facultyName) {
        this.facultyName = facultyName;
        chairs = new HashMap<>();
    }

    public void addSubmission(String chair, String person, Integer submissionCount) {
        if (!chairs.containsKey(chair)) {
            chairs.put(chair, new Chair(chair));
        }
        chairs.get(chair).addSubmission(person, submissionCount);
    }

    @JsonIgnore
    public String getFacultyName() {
        return facultyName;
    }

    @JsonProperty("data")
    public List<Chair> getChairs() {
        return new ArrayList<>(chairs.values());
    }

    @JsonProperty("submission_count")
    public Integer getSubmissionCount() {
        Integer result = 0;
        for (Chair chair : chairs.values()) {
            result += chair.getSubmissionCount();
        }
        return result;
    }
}

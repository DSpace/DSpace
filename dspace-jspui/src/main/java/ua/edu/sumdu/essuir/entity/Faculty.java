package ua.edu.sumdu.essuir.entity;

import org.json.JSONObject;
import org.json.simple.JSONArray;

import java.util.Date;
import java.util.HashMap;

public class Faculty {
    private String facultyName;
    private HashMap<String, Chair> chairs = new HashMap<>();

    public Faculty(String facultyName) {
        this.facultyName = facultyName;
    }

    public void addChair(Chair chair) {
        chairs.put(chair.getChairName(), chair);
    }

    public void addPerson(Person person) {
        if(!chairs.containsKey(person.getChairEntity())) {
            addChair(new Chair(person.getChairEntity()));
        }
        chairs.get(person.getChairEntity()).addPerson(person);
    }

    public int getSubmissionCountByDate(Date from, Date to) {
        int result = 0;
        for(Chair chair : chairs.values()) {
            result += chair.getSubmissionCountByDate(from, to);
        }
        return result;
    }
    public JSONObject generateJSONbyDate(Date from, Date to) {
        JSONObject result = new JSONObject();
        result.put("name", facultyName);
        result.put("submission_count", getSubmissionCountByDate(from, to));
        JSONArray chairs = new JSONArray();
        for (Chair chair : this.chairs.values()) {
            chairs.add(chair.generateJSONbyDate(from, to));
        }
        result.put("data", chairs);
        return result;
    }
}

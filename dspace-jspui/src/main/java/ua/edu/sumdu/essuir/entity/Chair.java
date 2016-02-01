package ua.edu.sumdu.essuir.entity;

import org.json.JSONObject;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Chair {
    private String chairName;
    private List<Person> persons = new ArrayList<>();

    public Chair(String chairName) {
        this.chairName = chairName;
    }

    public void addPerson(Person person) {
        persons.add(person);
    }

    public String getChairName() {
        return chairName;
    }

    public int getSubmissionCountByDate(Date from, Date to) {
        int result = 0;
        for (Person person : persons) {
            result += person.getSubmissionsCount(from, to);
        }
        return result;
    }

    public JSONObject generateJSONbyDate(Date from, Date to) {
        JSONObject result = new JSONObject();
        result.put("name", chairName);
        result.put("submission_count", getSubmissionCountByDate(from, to));
        JSONArray chairsPersons = new JSONArray();
        for (Person person : this.persons) {
            chairsPersons.add(person.generateJSONbyDate(from, to));
        }
        result.put("data", chairsPersons);
        return result;
    }
}

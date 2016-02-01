package ua.edu.sumdu.essuir.entity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Person {
    private String email;
    private ChairEntity chairEntity;
    private String name;
    private List<Date> submissions = new ArrayList<>();

    public Person(EPerson person, List<Submission> submissions) {
        this.email = person.getEmail();
        this.chairEntity = person.getChairEntity();
        if (submissions != null) {
            for (Submission submission : submissions) {
                this.submissions.add(submission.getDate());
            }

            name = submissions.get(submissions.size() - 1).getAuthor();
        } else {
            name = email;
        }
    }

    public int getSubmissionsCount(Date from, Date to) {
        int result = 0;
        for (Date submission : submissions) {
            if (submission.compareTo(from) >= 0 && submission.compareTo(to) <= 0) {
                result++;
            }
        }
        return result;
    }

    public String getChairEntity() {
        return (chairEntity == null) ? "" : chairEntity.getChairName();
    }

    public String getFaculty() {
        return (chairEntity == null) ? "" : chairEntity.getFacultyEntityName();
    }

    public JSONObject generateJSONbyDate(Date from, Date to) {
        JSONObject result = new JSONObject();
        result.put("name", name);
        result.put("submission_count", getSubmissionsCount(from, to));
        return result;
    }
}

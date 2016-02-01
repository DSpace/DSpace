package ua.edu.sumdu.essuir.service;

import org.dspace.core.ConfigurationManager;
import ua.edu.sumdu.essuir.entity.Submission;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class SubmissionHelper {
    private Statement statement;

    public SubmissionHelper() {
        try {
            Class.forName(ConfigurationManager.getProperty("db.driver"));
            Connection connection = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"), ConfigurationManager.getProperty("db.username"), ConfigurationManager.getProperty("db.password"));
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, List<Submission>> getSubmissionList() {
        String query = "SELECT MAX(text_value) AS text_value FROM item LEFT JOIN metadatavalue ON item_id = resource_id AND metadata_field_id = 28 AND text_value LIKE 'Submitted by%' WHERE in_archive AND resource_id IS NOT NULL GROUP BY resource_id";
        ArrayList<Submission> rows = new ArrayList<>();

        try {
            ResultSet resSet = statement.executeQuery(query);
            while (resSet.next()) {
                rows.add(new Submission(resSet.getString("text_value")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(rows);
        HashMap<String, List<Submission>> result = new HashMap<>();
        for(Submission submission : rows) {
            if(!result.containsKey(submission.getEmail())) {
                result.put(submission.getEmail(), new ArrayList<Submission>());
            }
            result.get(submission.getEmail()).add(submission);
        }
        return result;
    }
}

package ua.edu.sumdu.essuir.service;


import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.entity.Faculty;
import ua.edu.sumdu.essuir.entity.Item;

import javax.annotation.Resource;
import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportService {
    private static Logger log = Logger.getLogger(ReportService.class);
    @Resource
    private DatabaseService databaseService;

    @Resource
    private SpecialityReportFetcher specialityReportFetcher;

    private List<Faculty> populateDataFromQueryResult(CachedRowSet queryResult) {
        Map<String, Faculty> submissions = new HashMap<String, Faculty>();
        try {
            while (queryResult.next()) {
                if (!"null".equals(queryResult.getString("email"))) {
                    String faculty = queryResult.getString("faculty_name") == null ? " " : queryResult.getString("faculty_name");
                    String chair = queryResult.getString("chair_name") == null ? " " : queryResult.getString("chair_name");
                    String email = queryResult.getString("email");
                    String lastname = Optional.ofNullable(queryResult.getString("lastname")).orElse("null");
                    String firstname = Optional.ofNullable(queryResult.getString("firstname")).orElse("null");
                    String person = email;
                    if (!"null".equals(lastname) && !"null".equals(firstname)) {
                        person = String.format("%s %s", lastname, firstname);
                    }
                    Integer submissionCount = Integer.parseInt(queryResult.getString("submits"));
                    if (!submissions.containsKey(faculty)) {
                        submissions.put(faculty, new Faculty(faculty));
                    }
                    submissions.get(faculty).addSubmission(chair, person, submissionCount);
                }
            }
        } catch (SQLException ex) {

        }
        return new ArrayList<>(submissions.values());
    }

    public List<Faculty> getUsersSubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        String query = String.format("select eperson.eperson_id, email, lastname, firstname, chair_name, faculty_name, count(metadatavalue.resource_id) as submits " +
                "from eperson " +
                "left join chair on eperson.chair_id = chair.chair_id " +
                "left join faculty on faculty.faculty_id = chair.faculty_id " +
                "left join item on item.submitter_id = eperson_id and in_archive " +
                "left join metadatavalue on metadatavalue.resource_id = item.item_id and metadata_field_id = 11 " +
                "and text_value between '%d-%02d-%02d' and '%d-%02d-%02d' " +
                "group by eperson.eperson_id, chair_name, faculty_name ", from.getYear(), from.getMonth().getValue(), from.getDayOfMonth(), to.getYear(), to.getMonth().getValue(), to.getDayOfMonth());
        return populateDataFromQueryResult(databaseService.executeQuery(query));
    }


    public List<Item> getItemsInSpeciality(String pattern, LocalDate from, LocalDate to) {
        return specialityReportFetcher.getItemsInSpeciality(pattern, from, to);
    }

    public List<Item> getBacheoursWithoutSpeciality() {
        return specialityReportFetcher.getBachelorsWithoutSpeciality();
    }

    public List<Faculty> getSpecialitySubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        return specialityReportFetcher.getSpecialitySubmissionCountBetweenDates(from, to);
    }
}

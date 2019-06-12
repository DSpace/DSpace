package ua.edu.sumdu.essuir.utils;


import org.apache.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.essuir.cache.Author;
import ua.edu.sumdu.essuir.cache.AuthorCache;
import ua.edu.sumdu.essuir.entity.*;
import ua.edu.sumdu.essuir.repository.*;
import ua.edu.sumdu.essuir.service.DatabaseService;
import ua.edu.sumdu.essuir.service.ReportService;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EssuirUtils {
    private static final String MINIMAL_YEAR = "1964";
    private static DatabaseService databaseService;
    private static ChairRepository chairRepository;
    private static FacultyRepository facultyRepository;
    private static AuthorsRepository authorsRepository;
    private static ReportService reportService;


    private static Logger logger = Logger.getLogger(EssuirUtils.class);

    @Autowired
    public void setSpecialityStatisticsController(ReportService reportService) {
        EssuirUtils.reportService = reportService;
    }

    @Autowired
    public void setDatabaseService(DatabaseService databaseService) {
        EssuirUtils.databaseService = databaseService;
    }

    @Autowired
    public void setAuthorsRepository(AuthorsRepository authorsRepository) {
        EssuirUtils.authorsRepository = authorsRepository;
    }

    @Autowired
    public void setFacultyRepository(FacultyRepository facultyRepository) {
        EssuirUtils.facultyRepository = facultyRepository;
    }

    @Autowired
    public void setChairRepository(ChairRepository chairRepository) {
        EssuirUtils.chairRepository = chairRepository;
    }

    public static Map<String, Long> getTypesCount() {
        String query = "SELECT text_value, COUNT(*) AS cnts FROM metadatavalue WHERE metadata_field_id = 66 AND resource_id IN (SELECT item_id FROM item WHERE in_archive) GROUP BY text_value;";

        Result<Record> resSet = databaseService.executeQuery(query);
        return resSet.stream()
                .collect(Collectors.toMap(item -> item.get("text_value", String.class), item -> item.get("cnts", Long.class)));

    }

    private static String prevSessionLocale = "";
    private static java.util.Hashtable<String, String> typesTable = new java.util.Hashtable<String, String>();

    public static String getTypeLocalized(String type, String locale) {
        if (!locale.equals(prevSessionLocale)) {
            typesTable.clear();
            java.util.List vList = DCInputReader.getInputsReader(locale).getPairs("common_types");

            for (int i = 0; i < vList.size(); i += 2)
                typesTable.put((String) vList.get(i + 1), (String) vList.get(i));

            prevSessionLocale = locale;
        }

        String result = typesTable.get(type);
        return result == null ? type : result;
    }

    public static String getLanguageLocalized(String lang, String locale) {
        java.util.Hashtable<String, String> langTable = new java.util.Hashtable<String, String>();
        java.util.List vList = DCInputReader.getInputsReader(locale).getPairs("common_iso_languages");

        for (int i = 0; i < vList.size(); i += 2)
            langTable.put((String) vList.get(i + 1), (String) vList.get(i));

        String result = langTable.get(lang);
        return result == null ? lang : result;
    }


    public static String getMinimalPaperYear() {
        String query = "SELECT MIN(text_value) AS value FROM metadatavalue LEFT JOIN item ON resource_id = item_id WHERE metadata_field_id = 15 AND in_archive";
        Result<Record> result = databaseService.executeQuery(query);
        List<String> years = result.stream()
                .map(item -> item.getValue("value", String.class))
                .collect(Collectors.toList());

        if (years.isEmpty()) {
            return MINIMAL_YEAR;
        } else {
            return years.get(0);
        }
    }

    public static List<AuthorData> getLatestRegisteredAuthors(int limit) {
        String query = "SELECT m1.text_value AS firstname, m2.text_value AS lastname, chair_name, faculty_name, email " +
                "FROM eperson " +
                "LEFT JOIN (SELECT chair_id, chair_name, faculty_name " +
                "FROM chair " +
                "LEFT JOIN faculty " +
                "ON chair.faculty_id = faculty.faculty_id) b " +
                "ON eperson.chair_id = b.chair_id " +
                "LEFT JOIN metadatavalue AS m1 ON m1.resource_id = eperson.eperson_id AND m1.metadata_field_id = 129" +
                "LEFT JOIN metadatavalue AS m2 ON m2.resource_id = eperson.eperson_id AND m2.metadata_field_id = 130" +
                "ORDER BY eperson_id DESC " +
                "LIMIT " + limit;

        Result<Record> queryResult = databaseService.executeQuery(query);
        List<AuthorData> result = new LinkedList<>();

        return queryResult.stream()
                .map(item -> new AuthorData().setLastname(item.get("lastname", String.class))
                        .setFirstname(item.get("firstname", String.class))
                        .setEmail(item.get("email", String.class))
                        .setFaculty(item.get("faculty_name", String.class))
                        .setChair(item.get("chair_name", String.class)))
                .collect(Collectors.toList());

    }

    public static List<FacultyEntity> getFacultyList() {
        return facultyRepository.findAll();
    }

    public static Map<Integer, Map<Integer, String>> getChairListByFaculties() {
        List<ChairEntity> chairEntities = chairRepository.findAll();
        Map<Integer, Map<Integer, String>> chairs = new HashMap<>();

        for (ChairEntity chairEntity : chairEntities) {
            if (!chairs.containsKey(chairEntity.getFacultyEntityId())) {
                chairs.put(chairEntity.getFacultyEntityId(), new HashMap<Integer, String>());
            }
            chairs.get(chairEntity.getFacultyEntityId()).put(chairEntity.getId(), chairEntity.getChairName());
        }
        return chairs;
    }

    public static Integer getFacultyIdByChaidId(Integer chairId) {
        return chairRepository.findOne(chairId).getFacultyEntityId();
    }

    public static List<AuthorLocalization> getAllAuthors(String startWith) {
        return authorsRepository.findAuthorsStartedByLetter(startWith);
    }

    public static AuthorLocalization findAuthor(String surname, String initials) {
        return authorsRepository.findOne(new AuthorsLocalizationPK().setSurname_en(surname).setInitials_en(initials));
    }

    public static AuthorLocalization saveAuthor(AuthorLocalization author) {
        AuthorLocalization authorLocalization = authorsRepository.save(author);
        AuthorCache.update();
        return  authorLocalization;
    }

    public static AuthorLocalization findAuthor(Author author) {
        String surname = author.getName("en").split(", ")[0];
        String initials = author.getName("en").split(", ")[1];
        AuthorCache.update();
        return findAuthor(surname, initials);
    }
}

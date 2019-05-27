package ua.edu.sumdu.essuir.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.entity.*;
import ua.edu.sumdu.essuir.repository.MetadatavalueRepository;

import javax.annotation.Resource;
import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReportService {
    private static Logger log = Logger.getLogger(ReportService.class);
    @Resource
    private DatabaseService databaseService;
    @Resource
    private MetadatavalueRepository metadatavalueRepository;

    private Map<String, Faculty> populateDataFromQueryResult(CachedRowSet queryResult) {
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
        return submissions;
    }

    public Map<String, Faculty> getUsersSubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        String query = String.format("select eperson.eperson_id, email, lastname, firstname, chair_name, faculty_name, count(metadatavalue.resource_id) as submits " +
                "from eperson " +
                "left join chair on eperson.chair_id = chair.chair_id " +
                "left join faculty on faculty.faculty_id = chair.faculty_id " +
                "left join item on item.submitter_id = eperson_id and in_archive " +
                "left join metadatavalue on metadatavalue.resource_id = item.item_id and metadata_field_id = 11 " +
                "and text_value between '%d-%02d-%02d' and '%d-%02d-%02d' " +
                "group by eperson.eperson_id, chair_name, faculty_name", from.getYear(), from.getMonth().getValue(), from.getDayOfMonth(), to.getYear(), from.getMonth().getValue(), to.getDayOfMonth());
        return populateDataFromQueryResult(databaseService.executeQuery(query));
    }


    private Speciality extractSpecialityCode(String data) {
        FacultyEntity defaultFacultyEntity = new FacultyEntity.Builder().withId(-1).withName("-").build();
        ChairEntity defaultChairEntity = new ChairEntity.Builder().withId(-1).withChairName("-").withFacultyEntityName(defaultFacultyEntity).build();
        Speciality defaultSpecialityEntity = new Speciality.Builder().withId(-1).withName("-").withCode("-1").withChairEntity(defaultChairEntity).build();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(data);
            FacultyEntity.Builder facultyBuilder = new FacultyEntity.Builder(defaultFacultyEntity);
            if (jsonNode.has(0)) {
                facultyBuilder
                        .withId(jsonNode.get(0).get("code").asInt())
                        .withName(jsonNode.get(0).get("name").asText());
            }

            ChairEntity.Builder chairBuilder = new ChairEntity.Builder(defaultChairEntity)
                    .withFacultyEntityName(facultyBuilder.build());
            if (jsonNode.has(1)) {
                chairBuilder
                        .withId(jsonNode.get(1).get("code").asInt())
                        .withChairName(jsonNode.get(1).get("name").asText());
            }

            Speciality.Builder speciality = new Speciality.Builder(defaultSpecialityEntity)
                    .withChairEntity(chairBuilder.build());
            if (jsonNode.has(2)) {
                speciality.withName(jsonNode.get(2).get("name").asText())
                        .withCode(jsonNode.get(2).get("code").asText());
            } else {
                speciality.withName(chairBuilder.build().getChairName())
                        .withCode(chairBuilder.build().getId().toString());
            }
            return speciality.build();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(ex.getStackTrace());
        }
        return defaultSpecialityEntity;
    }

    private Map<Integer, Map<Integer, List<Metadatavalue>>> getBachelousPapersMetadata() {
        List<Integer> bachelousPaperIds = Stream.concat(
                metadatavalueRepository.findDistinctByTextValue("Bachelous paper").stream(),
                metadatavalueRepository.findDistinctByTextValue("Masters thesis").stream())
                .filter(item -> item.getItem().isPresent() && item.getItem().get().getInArchive())
                .map(Metadatavalue::getResourceId)
                .collect(Collectors.toList());

        List<Metadatavalue> metadatavaluesForBachelousPapers = metadatavalueRepository.findByResourceIdIn(bachelousPaperIds);
        return metadatavaluesForBachelousPapers.stream()
                .collect(Collectors.groupingBy(Metadatavalue::getResourceId, Collectors.groupingBy(Metadatavalue::getMetadataFieldId)));
    }

    private List<PaperDescription> getBachelousPapers() {
        Map<Integer, Map<Integer, List<Metadatavalue>>> bachelousPapersDescription = getBachelousPapersMetadata();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy dd", Locale.US);
        return bachelousPapersDescription.entrySet().stream()
                .filter(item -> item.getValue().containsKey(134) && item.getValue().containsKey(133))
                .map(item -> new PaperDescription.Builder()
                        .withResourceId(item.getKey())
                        .withSpeciality(extractSpecialityCode(item.getValue().get(133).get(0).getTextValue()))
                        .withAdded(LocalDate.parse(item.getValue().get(134).get(0).getTextValue() + " 01", formatter))
                        .build())
                .filter(paper -> paper.getSpeciality() != null)
                .collect(Collectors.toList());
    }

    public Map<Speciality, Long> getSpecialityStatistics(LocalDate from, LocalDate to) {
        return getBachelousPapers()
                .stream()
                .filter(paper -> paper.getAdded().isAfter(from) && paper.getAdded().isBefore(to))
                .collect(Collectors.groupingBy(PaperDescription::getSpeciality, Collectors.counting()));
    }

    public Map<String, Faculty> getSpecialitySubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        List<PaperDescription> bachelousPapers = getBachelousPapers();
        Map<String, Long> submissionInspeciality = bachelousPapers
                .stream()
                .filter(item -> item.getSpeciality() != null)
                .collect(Collectors.groupingBy(item -> item.getSpeciality().getName(), Collectors.counting()));

        Map<String, Faculty> result = new HashMap<>();

        for (PaperDescription paper : bachelousPapers) {
            if (paper.getAdded().isAfter(from) && paper.getAdded().isBefore(to)) {
                String faculty = paper.getSpeciality().getChairEntity().getFacultyEntityName();
                String chair = paper.getSpeciality().getChairEntity().getChairName();
                String speciality = paper.getSpeciality().getName();
                String specialityId = paper.getSpeciality().getName();
                if(!"-".equals(specialityId)) {
                    Long submissionCount = submissionInspeciality.get(specialityId);
                    result.putIfAbsent(faculty, new Faculty(faculty));
                    result.get(faculty).addSubmission(chair, speciality, submissionCount.intValue());
                }
            }
        }
        return result;
    }

    public Map<Integer, Map<Integer, List<Metadatavalue>>> getBacheoursWithoutSpeciality() {
        Map<Integer, Map<Integer, List<Metadatavalue>>> bacheloursItems = getBachelousPapersMetadata();

        Predicate<Map<Integer, List<Metadatavalue>>> isSpecialityAndPresentationDateSet = (metadataFields) -> metadataFields.containsKey(133) && metadataFields.containsKey(134);

        return bacheloursItems.entrySet()
                .stream()
                .filter(item -> isSpecialityAndPresentationDateSet.test(item.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Integer, Map<Integer, List<Metadatavalue>>> getItemsInSpeciality(String pattern) {
        List<Integer> itemIds = metadatavalueRepository.findDistinctByTextValueContaining(pattern).stream()
                .map(item -> item.getResourceId())
                .collect(Collectors.toList());

        return metadatavalueRepository.findByResourceIdIn(itemIds)
                .stream()
                .filter(item -> item.getItem().isPresent() && item.getItem().get().getInArchive())
                .collect(Collectors.groupingBy(Metadatavalue::getResourceId, Collectors.groupingBy(Metadatavalue::getMetadataFieldId)));
    }
}

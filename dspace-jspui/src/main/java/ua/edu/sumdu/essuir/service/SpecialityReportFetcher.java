package ua.edu.sumdu.essuir.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.sumdu.essuir.entity.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SpecialityReportFetcher {
    private static Logger log = Logger.getLogger(SpecialityReportFetcher.class);
    @Resource
    private DatabaseService databaseService;


    private BiPredicate<LocalDate, Pair<LocalDate, LocalDate>> isDateInRange = (date, range) -> date.isAfter(range.getLeft().minusDays(1)) && date.isBefore(range.getRight().plusDays(1));

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
        }
        return defaultSpecialityEntity;
    }

    @Transactional
    public List<Item> getBachelorsPapersMetadata() {
        return databaseService.fetchMastersAndBachelorsPapers();
    }

    private boolean isSpecialityNameAndPresentationDatePresented(Item item) {
        return !item.getSpecialityName().isEmpty() && !item.getPresentationDate().isEmpty();
    }

    public Map<Depositor, Long> getSpecialitySubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        List<Item> bachelorsPapersMetadata = getBachelorsPapersMetadata();
        return bachelorsPapersMetadata
                .stream()
                .filter(this::isSpecialityNameAndPresentationDatePresented)
                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
                .collect(Collectors.groupingBy(Item::getSpecialityName, Collectors.counting()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(item -> extractSpecialityCode(item.getKey()), Map.Entry::getValue));
    }

    public List<Item> getBachelorsWithoutSpeciality() {
        List<Item> items = getBachelorsPapersMetadata();
        return items.stream()
                .filter(item -> !isSpecialityNameAndPresentationDatePresented(item))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Item> getItemsInSpeciality(String pattern, LocalDate from, LocalDate to) {
        String[] depositor = pattern.split("//");
        List<Item> items = getBachelorsPapersMetadata();
        Predicate<String> isSpecialityNameContainsPattern = (specialityName) -> Stream.of(depositor).allMatch(specialityName::contains);
        return items.stream()
                .filter(this::isSpecialityNameAndPresentationDatePresented)
                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
                .filter(item -> isSpecialityNameContainsPattern.test(item.getSpecialityName()))
                .collect(Collectors.toList());
    }
}


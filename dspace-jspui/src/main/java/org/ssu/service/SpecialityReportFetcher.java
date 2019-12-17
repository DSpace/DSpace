package org.ssu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ssu.entity.ChairEntity;
import org.ssu.entity.FacultyEntity;
import org.ssu.entity.Speciality;
import org.ssu.entity.SpecialityDetailedInfo;
import org.ssu.entity.jooq.Faculty;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SpecialityReportFetcher {
    private static Logger log = Logger.getLogger(SpecialityReportFetcher.class);

    @Resource
    private ItemService essuirItemService;

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
                speciality.withName(chairBuilder.build().getName())
                        .withCode(chairBuilder.build().getId().toString());
            }
            return speciality.build();

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return defaultSpecialityEntity;
    }

    @Transactional
    public List<Speciality> getBachelorsPapersMetadata(Context context, LocalDate from, LocalDate to) throws IOException, SQLException {
        Function<String, Speciality> facultyEntityObjectMapper = (jsonData) -> {
            try {
                List<SpecialityDetailedInfo> specialityDetailedInfoList = new ObjectMapper().readValue(jsonData, new TypeReference<List<SpecialityDetailedInfo>>(){});
                if(specialityDetailedInfoList.size() == 3) {
                    FacultyEntity faculty = new FacultyEntity.Builder()
                            .withId(specialityDetailedInfoList.get(0).getCode())
                            .withName(specialityDetailedInfoList.get(0).getName())
                            .build();

                    ChairEntity chair = new ChairEntity.Builder()
                            .withFacultyEntityName(faculty)
                            .withChairName(specialityDetailedInfoList.get(1).getName())
                            .withId(specialityDetailedInfoList.get(1).getCode())
                            .build();

                    return new Speciality.Builder()
                            .withChairEntity(chair)
                            .withId(specialityDetailedInfoList.get(2).getCode())
                            .withName(specialityDetailedInfoList.get(2).getName())
                            .build();
                }
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };
        Map<UUID, LocalDate> allDatesAvailable = essuirItemService.getAllDatesAvailable(context);
        return essuirItemService.fetchMastersAndBachelorsPapers()
                .entrySet()
                .stream()
                .filter(submission -> isDateInRange.test(allDatesAvailable.getOrDefault(submission.getKey(), LocalDate.MIN), Pair.of(from, to)))
                .map(Map.Entry::getValue)
                .map(facultyEntityObjectMapper)
                .collect(Collectors.toList());
    }

//    private boolean isSpecialityNameAndPresentationDatePresented(Item item) {
//        return !item.getSpecialityName().isEmpty() && !item.getPresentationDate().isEmpty();
//    }

    public List<Pair<Speciality, Long>> getSpecialitySubmissionCountBetweenDates(Context context, LocalDate from, LocalDate to) throws IOException, SQLException {
        return Seq.seq(getBachelorsPapersMetadata(context, from, to))
                .filter(Objects::nonNull)
                .grouped(item -> item, Collectors.counting())
                .map(item -> Pair.of(item.v1(), item.v2()))
                .toList();
    }
//
//    public List<Item> getBachelorsWithoutSpeciality() {
//        List<Item> items = getBachelorsPapersMetadata();
//        return items.stream()
//                .filter(item -> !isSpecialityNameAndPresentationDatePresented(item))
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public List<Item> getItemsInSpeciality(String pattern, LocalDate from, LocalDate to) {
//        String[] depositor = pattern.split("//");
//        List<Item> items = getBachelorsPapersMetadata();
//        Predicate<String> isSpecialityNameContainsPattern = (specialityName) -> Stream.of(depositor).allMatch(specialityName::contains);
//        return items.stream()
//                .filter(this::isSpecialityNameAndPresentationDatePresented)
//                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
//                .filter(item -> isSpecialityNameContainsPattern.test(item.getSpecialityName()))
//                .collect(Collectors.toList());
//    }
}

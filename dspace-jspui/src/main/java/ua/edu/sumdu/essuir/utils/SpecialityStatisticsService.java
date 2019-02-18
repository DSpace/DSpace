package ua.edu.sumdu.essuir.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import ua.edu.sumdu.essuir.entity.*;
import ua.edu.sumdu.essuir.repository.MetadatavalueRepository;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class SpecialityStatisticsService {
    private static Logger log = Logger.getLogger(SpecialityStatisticsService.class);
    @Resource
    private MetadatavalueRepository metadatavalueRepository;

    private Speciality extractSpecialityCode(String data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(data);
            FacultyEntity faculty = new FacultyEntity.Builder()
                    .withId(jsonNode.get(0).get("code").asInt())
                    .withName(jsonNode.get(0).get("name").asText())
                    .build();
            ChairEntity chair = new ChairEntity.Builder()
                    .withId(jsonNode.get(1).get("code").asInt())
                    .withChairName(jsonNode.get(1).get("name").asText())
                    .withFacultyEntityName(faculty)
                    .build();
            return new Speciality.Builder()
                    .withName(jsonNode.get(2).get("name").asText())
                    .withCode(jsonNode.get(2).get("code").asText())
                    .withChairEntity(chair)
                    .build();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(ex.getStackTrace());
        }
        FacultyEntity defaultFacultyEntity = new FacultyEntity.Builder().withId(-1).withName("-").build();
        ChairEntity defaultChairEntity = new ChairEntity.Builder().withId(-1).withChairName("-").withFacultyEntityName(defaultFacultyEntity).build();
        return new Speciality.Builder().withId(-1).withName("-").withChairEntity(defaultChairEntity).build();

    }

    private List<PaperDescription> getBachelousPapers() {
        List<Integer> bachelousPaperIds = Stream.concat(
                metadatavalueRepository.findDistinctByTextValue("Bachelous paper").stream(),
                metadatavalueRepository.findDistinctByTextValue("Masters thesis").stream())
                .map(Metadatavalue::getResourceId)
                .collect(Collectors.toList());

        List<Metadatavalue> metadatavaluesForBachelousPapers = metadatavalueRepository.findByResourceIdIn(bachelousPaperIds);
        Map<Integer, Map<Integer, List<Metadatavalue>>> bachelousPapersDescription = metadatavaluesForBachelousPapers.stream()
                .collect(Collectors.groupingBy(Metadatavalue::getResourceId, Collectors.groupingBy(Metadatavalue::getMetadataFieldId)));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy dd", Locale.US);

        return bachelousPaperIds.stream()
                .filter(id -> bachelousPapersDescription.containsKey(id) && bachelousPapersDescription.get(id).containsKey(134) && bachelousPapersDescription.get(id).containsKey(133))

                .map(id -> new PaperDescription.Builder()
                        .withResourceId(id)
                        .withSpeciality(extractSpecialityCode(bachelousPapersDescription.get(id).get(133).get(0).getTextValue()))
                        .withAdded(LocalDate.parse(bachelousPapersDescription.get(id).get(134).get(0).getTextValue() + " 01", formatter))
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
}

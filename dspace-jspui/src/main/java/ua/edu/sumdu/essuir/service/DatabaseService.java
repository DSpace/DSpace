package ua.edu.sumdu.essuir.service;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.entity.ChairEntity;
import ua.edu.sumdu.essuir.entity.FacultyEntity;
import ua.edu.sumdu.essuir.entity.Item;
import ua.edu.sumdu.essuir.entity.jooq.Chair;
import ua.edu.sumdu.essuir.entity.jooq.EPerson;
import ua.edu.sumdu.essuir.entity.jooq.Metadatavalue;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service("databaseService")
public class DatabaseService {
    private static final EPerson EPERSON = EPerson.TABLE;
    private static final Chair CHAIR = Chair.TABLE;
    private static final ua.edu.sumdu.essuir.entity.jooq.Item ITEM = ua.edu.sumdu.essuir.entity.jooq.Item.TABLE;
    private static final ua.edu.sumdu.essuir.entity.jooq.Faculty FACULTY = ua.edu.sumdu.essuir.entity.jooq.Faculty.TABLE;
    private static final Metadatavalue METADATAVALUE = Metadatavalue.TABLE;
    private final int METADATA_FIELD_ID_SPECIALITY = 133;
    private final int METADATA_FIELD_ID_PRESENTATION_DATE = 134;
    private final int METADATA_FIELD_ID_LINK = 25;
    private final int METADATA_FIELD_ID_TITLE = 64;
    private final int METADATA_FIELD_ID_DATE_AVAILABLE = 12;
    @Resource
    private DSLContext dsl;

    private Map<Integer, String> extractMetadata(int fieldId) {
        return dsl.select(ITEM.itemId, METADATAVALUE.value)
                .from(ITEM)
                .join(METADATAVALUE).on(ITEM.itemId.eq(METADATAVALUE.resourceId))
                .where(METADATAVALUE.place.eq(1).and(METADATAVALUE.metadataFieldId.eq(fieldId)).and(METADATAVALUE.resourceTypeId.eq(2)))
                .fetch()
                .stream()
                .collect(Collectors.toMap(item -> item.get(ITEM.itemId), item -> item.get(METADATAVALUE.value)));

    }

    public List<Item> fetchMastersAndBachelorsPapers() {
        Set<Integer> paperIds = dsl.selectDistinct(METADATAVALUE.resourceId)
                .from(METADATAVALUE)
                .where(METADATAVALUE.value.eq("Bachelous paper").or(METADATAVALUE.value.eq("Masters thesis")))
                .fetchSet(METADATAVALUE.resourceId);
        return fetchItemsInArchive()
                .stream()
                .filter(item -> paperIds.contains(item.getItemId()))
                .collect(Collectors.toList());
    }
    public List<Item> fetchItemsInArchive() {
        Map<Integer, String> dateAvailable = extractMetadata(METADATA_FIELD_ID_DATE_AVAILABLE);
        Map<Integer, String> link = extractMetadata(METADATA_FIELD_ID_LINK);
        Map<Integer, String> presentationDate = extractMetadata(METADATA_FIELD_ID_PRESENTATION_DATE);
        Map<Integer, String> speciality = extractMetadata(METADATA_FIELD_ID_SPECIALITY);
        Map<Integer, String> title = extractMetadata(METADATA_FIELD_ID_TITLE);

        BiFunction<Record, Map<Integer, String>, String> getMetadataForRecord = (record, map) -> map.getOrDefault(record.get(ITEM.itemId), "");

        return dsl.select(ITEM.itemId, EPERSON.firstname, EPERSON.lastname, EPERSON.email, CHAIR.chairName, CHAIR.chairId, FACULTY.facultyName, FACULTY.facultyId)
                .from(ITEM)
                .join(EPERSON).on(EPERSON.epersonId.eq(ITEM.submitterId))
                .join(CHAIR).on(EPERSON.chairId.eq(CHAIR.chairId))
                .join(FACULTY).on(CHAIR.facultyId.eq(FACULTY.facultyId))
                .where(ITEM.inArchive.isTrue())
                .fetch(item -> mapper(item)
                        .withTitle(title.getOrDefault(item.get(ITEM.itemId), ""))
                        .withDateAvailable(dateAvailable.getOrDefault(item.get(ITEM.itemId), ""))
                        .withLink(link.getOrDefault(item.get(ITEM.itemId), ""))
                        .withPresentationDate(presentationDate.getOrDefault(item.get(ITEM.itemId), ""))
                        .withSpecialityName(speciality.getOrDefault(item.get(ITEM.itemId), ""))
                        .build())
                .stream()
                .collect(Collectors.toList());
    }

    private Item.Builder mapper(Record record) {
        FacultyEntity facultyEntity = new FacultyEntity.Builder()
                .withId(record.get(FACULTY.facultyId))
                .withName(record.get(FACULTY.facultyName))
                .build();
        ChairEntity chairEntity = new ChairEntity.Builder()
                .withChairName(record.get(CHAIR.chairName))
                .withId(record.get(CHAIR.chairId))
                .withFacultyEntityName(facultyEntity)
                .build();

        ua.edu.sumdu.essuir.entity.EPerson eperson = new ua.edu.sumdu.essuir.entity.EPerson.Builder()
                .withFirstname(record.get(EPERSON.firstname))
                .withLastname(record.get(EPERSON.lastname))
                .withEmail(record.get(EPERSON.email))
                .withChairEntity(chairEntity)
                .build();

        return new Item.Builder()
                .withItemId(record.get(ITEM.itemId))
                .withSubmitter(eperson);
    }

    public Result<Record> executeQuery(String sql) {
        return dsl.fetch(sql);
    }

}

package org.ssu.service;

import com.google.common.collect.Lists;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.jooq.*;
import org.springframework.stereotype.Service;
import org.ssu.entity.ChairEntity;
import org.ssu.entity.EssuirEperson;
import org.ssu.entity.FacultyEntity;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.*;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EpersonService {
    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Resource
    private DSLContext dsl;

    private static final org.ssu.entity.jooq.EPerson EPERSON = org.ssu.entity.jooq.EPerson.TABLE;
    private static final org.ssu.entity.jooq.Chair CHAIR = org.ssu.entity.jooq.Chair.TABLE;
    private static final org.ssu.entity.jooq.Faculty FACULTY = org.ssu.entity.jooq.Faculty.TABLE;

    public List<EssuirEperson> getLatestRegisteredUsers(Context context, int limit) throws SQLException {
        return ePersonService.findAll(context, EPerson.ID)
                .stream()
                .sorted(Comparator.comparing(EPerson::getLegacyId).reversed())
                .limit(limit)
                .map(this::extendEpersonInformation)
                .collect(Collectors.toList());
    }

    public EssuirEperson extendEpersonInformation(EPerson eperson) {
        Function<Record, FacultyEntity> extractFacultyEntityInformation = (record) -> new FacultyEntity.Builder().withId(record.get(FACULTY.facultyId)).withName(record.get(FACULTY.facultyName)).build();
        Function<Record, ChairEntity> buildChairEntity = (record) -> new ChairEntity.Builder().withId(record.get(CHAIR.chairId)).withChairName(record.get(CHAIR.chairName)).withFacultyEntityName(extractFacultyEntityInformation.apply(record)).build();

        return dsl.select(EPERSON.chairId, EPERSON.position, FACULTY.asterisk(), CHAIR.asterisk())
                .from(EPERSON)
                .leftJoin(CHAIR).on(CHAIR.chairId.eq(EPERSON.chairId))
                .leftJoin(FACULTY).on(FACULTY.facultyId.eq(CHAIR.facultyId))
                .where(EPERSON.uuid.eq(eperson.getID()))
                .fetchOne()
                .map(record ->
                        new EssuirEperson.Builder()
                                .withEPerson(eperson)
                                .withChairEntity(buildChairEntity.apply(record))
                                .withPosition(record.get(EPERSON.position))
                                .build()
                );

    }
}

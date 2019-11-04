package org.ssu.service;

import org.dspace.eperson.EPerson;
import org.jooq.*;
import org.springframework.stereotype.Service;
import org.ssu.entity.ChairEntity;
import org.ssu.entity.EssuirEperson;
import org.ssu.entity.FacultyEntity;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
public class EpersonService {
    @Resource
    private DSLContext dsl;

    private static final org.ssu.entity.jooq.EPerson EPERSON = org.ssu.entity.jooq.EPerson.TABLE;
    private static final org.ssu.entity.jooq.Chair CHAIR = org.ssu.entity.jooq.Chair.TABLE;
    private static final org.ssu.entity.jooq.Faculty FACULTY = org.ssu.entity.jooq.Faculty.TABLE;

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

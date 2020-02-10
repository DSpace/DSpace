package org.ssu.service;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EpersonService {
    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final org.ssu.entity.jooq.EPerson EPERSON = org.ssu.entity.jooq.EPerson.TABLE;
    private static final org.ssu.entity.jooq.Chair CHAIR = org.ssu.entity.jooq.Chair.TABLE;
    private static final org.ssu.entity.jooq.Faculty FACULTY = org.ssu.entity.jooq.Faculty.TABLE;
    @Resource
    private DSLContext dsl;

    public List<EPerson> getLatestRegisteredUsers(Context context, int limit) throws SQLException {
        return ePersonService.findAll(context, EPerson.ID)
                .stream()
                .sorted(Comparator.comparing(EPerson::getLegacyId).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}

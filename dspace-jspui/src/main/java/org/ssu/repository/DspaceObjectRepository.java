package org.ssu.repository;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

@Service
public class DspaceObjectRepository {
    private static final org.ssu.entity.jooq.DspaceObject DSPACE_OBJECT = org.ssu.entity.jooq.DspaceObject.TABLE;

    @Resource
    private DSLContext dsl;

    public void insertUuid(UUID uuid) {
        dsl.insertInto(DSPACE_OBJECT)
                .set(DSPACE_OBJECT.uuid, uuid)
                .onDuplicateKeyIgnore()
                .execute();
    }
}

package org.ssu.repository;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class MetadatavalueRepository {
    private static final org.ssu.entity.jooq.Metadatavalue METADATAVALUE = org.ssu.entity.jooq.Metadatavalue.TABLE;
    private static final org.ssu.entity.jooq.Handle HANDLE = org.ssu.entity.jooq.Handle.TABLE;

    private final Integer METADATAVALUE_TITLE_FIELD_ID = 64;
    private final Integer METADATAVALUE_LINK_FIELD_ID = 25;
    private final Integer METADATAVALUE_AUTHORS_FIELD_ID = 3;
    @Resource
    private DSLContext dsl;

    public List<Item> findAll(Context context) throws SQLException {
        Iterator<Item> all = ContentServiceFactory.getInstance().getItemService().findAll(context);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(all, Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    private String getItemMetadataByFieldId(UUID uuid, int fieldTypeId) {
        return dsl.select(METADATAVALUE.value)
                .from(METADATAVALUE)
                .where(METADATAVALUE.metadataFieldId.eq(fieldTypeId).and(METADATAVALUE.dspaceObjectId.eq(uuid)))
                .fetchOne(METADATAVALUE.value);
    }

    public String getItemTitleByDspaceObjectId(UUID uuid) {
        return getItemMetadataByFieldId(uuid, METADATAVALUE_TITLE_FIELD_ID);
    }

    public String getItemLinkByDspaceObjectId(UUID uuid) {
        return getItemMetadataByFieldId(uuid, METADATAVALUE_LINK_FIELD_ID);
    }

    public List<Pair<String, UUID>> getItemAuthorAndItemIdMapping() {
        return dsl.select(METADATAVALUE.value, METADATAVALUE.dspaceObjectId)
                .from(METADATAVALUE)
                .where(METADATAVALUE.metadataFieldId.eq(METADATAVALUE_AUTHORS_FIELD_ID))
                .fetch()
                .stream()
                .map(item -> Pair.of(item.get(METADATAVALUE.value), item.get(METADATAVALUE.dspaceObjectId)))
                .collect(Collectors.toList());
    }

    public Map<UUID, String> selectMetadataByFieldId(Integer fieldId) {
        return dsl.select(METADATAVALUE.dspaceObjectId, METADATAVALUE.value)
                .from(METADATAVALUE)
                .where(METADATAVALUE.metadataFieldId.eq(fieldId))
                .fetchStream()
                .collect(Collectors.toMap(item -> item.get(METADATAVALUE.dspaceObjectId), item -> item.get(METADATAVALUE.value), (a, b) -> a));
    }
}

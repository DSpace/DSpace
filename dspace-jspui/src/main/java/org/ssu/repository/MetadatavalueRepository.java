package org.ssu.repository;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class MetadatavalueRepository {
    private static final org.ssu.entity.jooq.Item ITEM = org.ssu.entity.jooq.Item.TABLE;
    private static final org.ssu.entity.jooq.Statistics STATISTICS = org.ssu.entity.jooq.Statistics.TABLE;
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

    private String getItemMetadataByFieldId(int itemId, int fieldTypeId) {
        return dsl.select(METADATAVALUE.value)
                .from(HANDLE)
                .join(METADATAVALUE).on(METADATAVALUE.dspaceObjectId.eq(HANDLE.resourceId))
                .where(METADATAVALUE.metadataFieldId.eq(fieldTypeId).and(HANDLE.resourceLegacyId.eq(itemId)))
                .fetchOne(METADATAVALUE.value);
    }
    public String getItemTitleByItemId(int itemId) {
        return getItemMetadataByFieldId(itemId, METADATAVALUE_TITLE_FIELD_ID);
    }

    public String getItemLinkByItemId(int itemId) {
        return getItemMetadataByFieldId(itemId, METADATAVALUE_LINK_FIELD_ID);
    }

    public List<Pair<String, Integer>> getItemAuthorAndItemIdMapping() {
        return dsl.select(METADATAVALUE.value, HANDLE.resourceLegacyId)
                .from(METADATAVALUE)
                .leftJoin(HANDLE).on(METADATAVALUE.dspaceObjectId.eq(HANDLE.resourceId))
                .where(METADATAVALUE.metadataFieldId.eq(METADATAVALUE_AUTHORS_FIELD_ID))
                .fetch()
                .stream()
                .map(item -> Pair.of(item.get(METADATAVALUE.value), item.get(HANDLE.resourceLegacyId)))
                .collect(Collectors.toList());
    }
}

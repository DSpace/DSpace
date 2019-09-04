package org.ssu.repository;

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

    @Resource
    private DSLContext dsl;

    public List<Item> findAll(Context context) throws SQLException {
        Iterator<Item> all = ContentServiceFactory.getInstance().getItemService().findAll(context);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(all, Spliterator.ORDERED), false)
                .collect(Collectors.toList());

    }

    public String getItemTitleByItemId(int itemId) {
        return dsl.select(METADATAVALUE.value)
                .from(HANDLE)
                .join(METADATAVALUE).on(METADATAVALUE.dspaceObjectId.eq(HANDLE.resourceId))
                .where(METADATAVALUE.metadataFieldId.eq(64).and(HANDLE.resourceLegacyId.eq(itemId)))
                .fetchOne()
                .value1();
    }

    public String getItemLinkByItemId(int itemId) {
        return dsl.select(METADATAVALUE.value)
                .from(HANDLE)
                .join(METADATAVALUE).on(METADATAVALUE.dspaceObjectId.eq(HANDLE.resourceId))
                .where(METADATAVALUE.metadataFieldId.eq(25).and(HANDLE.resourceLegacyId.eq(itemId)))
                .fetchOne()
                .value1();
    }

    public Integer getMaximumItemId() {
        return dsl.select(DSL.max(HANDLE.resourceLegacyId))
                .from(HANDLE)
                .fetchOne().value1();
    }
}

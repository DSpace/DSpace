package org.ssu.types;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReaderException;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.ssu.LocalizedInputsReader;
import org.ssu.entity.ItemTypeResponse;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TypeLocalization {
    private static final org.ssu.entity.jooq.Metadatavalue METADATAVALUE = org.ssu.entity.jooq.Metadatavalue.TABLE;
    private static final org.ssu.entity.jooq.Item ITEM = org.ssu.entity.jooq.Item.TABLE;
    private static final org.ssu.entity.jooq.Handle HANDLE = org.ssu.entity.jooq.Handle.TABLE;
    private static Logger logger = Logger.getLogger(TypeLocalization.class);
    @Resource
    private DSLContext dsl;
    private Map<String, String> typesTable = new HashMap<>();

    public Map<String, Integer> getTypesCount() {

        return dsl.select(METADATAVALUE.value, DSL.count())
                .from(ITEM)
                .join(HANDLE).on(HANDLE.resourceLegacyId.eq(ITEM.itemId))
                .join(METADATAVALUE).on(METADATAVALUE.dspaceObjectId.eq(HANDLE.resourceId))
                .where(ITEM.inArchive.isTrue().and(METADATAVALUE.metadataFieldId.eq(66)))
                .groupBy(METADATAVALUE.value)
                .fetch()
                .stream()
                .collect(Collectors.toMap(item -> item.get(METADATAVALUE.value), item -> item.get(DSL.count())));
    }

    private void updateTypeLocalizationTable(String locale) {
        typesTable.clear();
        List<String> typesList = null;
        try {
            typesList = new LocalizedInputsReader().getInputsReader(locale).getPairs("common_types");
        } catch (DCInputsReaderException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < typesList.size(); i += 2)
            typesTable.put(typesList.get(i + 1), typesList.get(i));
    }

    public String getTypeLocalized(String type, String locale) {
        updateTypeLocalizationTable(locale);
        return typesTable.getOrDefault(type, type);
    }

    public List<ItemTypeResponse> getSubmissionStatisticsByType(String locale) {

        return getTypesCount().entrySet()
                .stream()
                .map(item -> new ItemTypeResponse.Builder().withTitle(getTypeLocalized(item.getKey(), locale)).withCount(item.getValue()).withSearchQuery(item.getKey()).build())
                .collect(Collectors.toList());

    }
}

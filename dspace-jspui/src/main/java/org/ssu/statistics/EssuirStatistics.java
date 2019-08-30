package org.ssu.statistics;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

@Service
public class EssuirStatistics {
    private static final org.ssu.entity.jooq.Item ITEM = org.ssu.entity.jooq.Item.TABLE;
    private static final org.ssu.entity.jooq.Statistics STATISTICS = org.ssu.entity.jooq.Statistics.TABLE;
    private static final org.ssu.entity.jooq.Metadatavalue METADATAVALUE = org.ssu.entity.jooq.Metadatavalue.TABLE;
    private static final org.ssu.entity.jooq.Handle HANDLE = org.ssu.entity.jooq.Handle.TABLE;

    @Resource
    private DSLContext dsl;

    private String getLastUpdate() {

        return dsl.select(METADATAVALUE.value)
                .from(ITEM)
                .join(HANDLE).on(HANDLE.resourceLegacyId.eq(ITEM.itemId))
                .join(METADATAVALUE).on(METADATAVALUE.dspaceObjectId.eq(HANDLE.resourceId))
                .where(ITEM.inArchive.isTrue().and(METADATAVALUE.metadataFieldId.eq(11)).and(METADATAVALUE.place.eq(1)))
                .orderBy(METADATAVALUE.value.desc())
                .limit(1)
                .fetchAny(METADATAVALUE.value);
    }

    public StatisticsData getTotalStatistic() {
        Integer count = dsl.select(DSL.count())
                .from(ITEM)
                .where(ITEM.inArchive.isTrue())
                .fetchAny(DSL.count());

        return new StatisticsData.Builder()
                .withLastUpdate(Arrays.stream(getLastUpdate().split("T")).findFirst().orElse(LocalDate.now().toString()))
                .withTotalCount(count)
                .withTotalDownloads(getStatistics(0))
                .withTotalViews(getStatistics(-1))
                .build();
    }

    private Long getStatistics(int sequenceIdComparator) {
        Condition condition = (sequenceIdComparator >= 0) ? STATISTICS.sequenceId.greaterOrEqual(0) : STATISTICS.sequenceId.lessThan(0);
        return dsl.select(DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(condition)
                .fetchOptional(DSL.sum(STATISTICS.viewCount))
                .map(BigDecimal::longValue)
                .orElse(-1L);
    }
}

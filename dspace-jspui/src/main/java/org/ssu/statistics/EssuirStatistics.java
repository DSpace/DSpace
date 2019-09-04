package org.ssu.statistics;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.ssu.localization.AuthorsCache;
import org.ssu.repository.MetadatavalueRepository;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EssuirStatistics {
    private static final org.ssu.entity.jooq.Item ITEM = org.ssu.entity.jooq.Item.TABLE;
    private static final org.ssu.entity.jooq.Statistics STATISTICS = org.ssu.entity.jooq.Statistics.TABLE;
    private static final org.ssu.entity.jooq.Metadatavalue METADATAVALUE = org.ssu.entity.jooq.Metadatavalue.TABLE;
    private static final org.ssu.entity.jooq.Handle HANDLE = org.ssu.entity.jooq.Handle.TABLE;

    @Resource
    private MetadatavalueRepository metadatavalueRepository;

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
                .withTotalDownloads(getTotalDownloads())
                .withTotalViews(getTotalViews())
                .build();
    }

    public Map<String, Integer> getStatisticsByType() {
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

    private Map<Integer, Long> getDownloadsStatistics() {
        return getStatistics(STATISTICS.sequenceId.greaterOrEqual(0));
    }

    private Map<Integer, Long> getViewStatistics() {
        return getStatistics(STATISTICS.sequenceId.lessThan(0));
    }

    private Long getTotalViews() {
        return getViewStatistics()
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    private Long getTotalDownloads() {
        return getDownloadsStatistics()
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    private Map<Integer, Long> getStatistics(Condition condition) {
        return dsl.select(STATISTICS.itemId, DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(condition)
                .groupBy(STATISTICS.itemId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(item -> item.get(STATISTICS.itemId), item -> item.get(DSL.sum(STATISTICS.viewCount)).longValue()));
    }

    public List<org.ssu.entity.Item> topPublications(int limit) throws SQLException {
        List<Pair<Integer, Long>> downloads = getDownloadsStatistics().entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(item -> Pair.of(item.getKey(), item.getValue()))
                .collect(Collectors.toList());

        return downloads.stream()
                .map(item -> new org.ssu.entity.Item.Builder()
                        .withItemId(item.getKey())
                        .withDownloadCount(item.getValue())
                        .withName(metadatavalueRepository.getItemTitleByItemId(item.getKey()))
                        .withLink(metadatavalueRepository.getItemLinkByItemId(item.getKey()))
                        .build())
                .collect(Collectors.toList());
    }


}

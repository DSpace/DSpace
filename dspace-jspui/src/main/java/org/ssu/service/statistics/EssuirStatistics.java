package org.ssu.service.statistics;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.dspace.browse.*;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.lambda.Seq;
import org.springframework.stereotype.Service;

import org.ssu.entity.AuthorLocalization;
import org.ssu.entity.statistics.StatisticsData;
import org.ssu.service.AuthorsService;
import org.ssu.service.GeoIpService;
import org.ssu.service.localization.AuthorsCache;
import org.ssu.repository.MetadatavalueRepository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EssuirStatistics {
    private static final org.ssu.entity.jooq.Item ITEM = org.ssu.entity.jooq.Item.TABLE;
    private static final org.ssu.entity.jooq.Statistics STATISTICS = org.ssu.entity.jooq.Statistics.TABLE;
    private static final org.ssu.entity.jooq.Metadatavalue METADATAVALUE = org.ssu.entity.jooq.Metadatavalue.TABLE;
//    private static final org.ssu.entity.jooq.Handle HANDLE = org.ssu.entity.jooq.Handle.TABLE;
private org.dspace.content.service.ItemService dspaceItemService = ContentServiceFactory.getInstance().getItemService();

    @Resource
    private MetadatavalueRepository metadatavalueRepository;

    @Resource
    private AuthorsService authorsService;

    @Resource
    private DSLContext dsl;

    @Resource
    private GeoIpService geoIpService;

    enum StatisticType {
        VIEWS, DOWNLOADS;
    }

    private String getLastUpdate(Context context) {
        try {
            BrowseEngine be = new BrowseEngine(context);
            BrowserScope bs = new BrowserScope(context);
            BrowseIndex bi = BrowseIndex.getItemBrowseIndex();
            bs.setBrowseIndex(bi);
            bs.setOrder(SortOption.DESCENDING);
            bs.setResultsPerPage(1);

            bs.setSortBy(SortOption.getSortOptions()
                    .stream()
                    .filter(option -> option.getName().equals("dateaccessioned"))
                    .map(option -> option.getNumber())
                    .findFirst()
                    .orElse(0));
            BrowseInfo browse = be.browseMini(bs);
            Item item = browse.getBrowseItemResults().stream().findFirst().orElseThrow(NullPointerException::new);
            return dspaceItemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "accessioned", Item.ANY)
                    .stream()
                    .findFirst()
                    .map(MetadataValue::getValue)
                    .orElseThrow(NullPointerException::new);
        } catch (BrowseException e) {
            e.printStackTrace();
        } catch (SortException e) {
            e.printStackTrace();
        }
        return LocalDate.now().toString();
    }

    public StatisticsData getTotalStatistic(Context context) {
        Integer count = dsl.select(DSL.count())
                .from(ITEM)
                .where(ITEM.inArchive.isTrue())
                .fetchAny(DSL.count());

        return new StatisticsData.Builder()
                .withLastUpdate(Arrays.stream(getLastUpdate(context).split("T")).findFirst().orElse(LocalDate.now().toString()))
                .withTotalCount(count)
                .withTotalDownloads(getTotalDownloads())
                .withTotalViews(getTotalViews())
                .build();
    }

    public Map<String, Integer> getStatisticsByType() {
        return dsl.select(METADATAVALUE.value, DSL.count())
                .from(ITEM)
                .join(METADATAVALUE).on(METADATAVALUE.dspaceObjectId.eq(ITEM.uuid))
                .where(METADATAVALUE.metadataFieldId.eq(66).and(ITEM.inArchive.isTrue()))
                .groupBy(METADATAVALUE.value)
                .fetch()
                .stream()
                .collect(Collectors.toMap(item -> item.get(METADATAVALUE.value), item -> item.get(DSL.count())));
    }

    private Map<Integer, Long> getDownloadsStatistics() {
        return getStatistics()
                .entrySet()
                .stream()
                .filter(item -> item.getKey().getRight().equals(StatisticType.DOWNLOADS))
                .collect(Collectors.toMap(item -> item.getKey().getLeft(), item -> item.getValue(), (a, b) -> a));
    }

    private Integer getTotalViews() {
        return dsl.select(DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(STATISTICS.sequenceId.lessThan(0))
                .fetchSingle(DSL.sum(STATISTICS.viewCount))
                .intValue();
    }

    private Integer getTotalDownloads() {
        return dsl.select(DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(STATISTICS.sequenceId.greaterOrEqual(0))
                .fetchSingle(DSL.sum(STATISTICS.viewCount))
                .intValue();    }

    private Map<Pair<Integer, StatisticType>, Long> getStatistics() {

        return Seq.seq(dsl.select(STATISTICS.itemId, STATISTICS.sequenceId, STATISTICS.viewCount)
                .from(STATISTICS)
                .fetch())
                .map(item -> Triple.of(item.get(STATISTICS.itemId), item.get(STATISTICS.sequenceId) >= 0 ? StatisticType.DOWNLOADS : StatisticType.VIEWS, item.get(STATISTICS.viewCount)))
                .grouped(tuple -> Pair.of(tuple.getLeft(), tuple.getMiddle()), Collectors.summingLong(item -> item.getRight()))
                .toMap(item -> item.v1(), item -> item.v2());
    }

    public List<org.ssu.entity.Item> topPublications(int limit) {
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

    public List<Pair<AuthorLocalization, Long>> topAuthors(int limit) {
        Map<Integer, Long> downloads = getDownloadsStatistics();
        List<Pair<String, Integer>> authors = metadatavalueRepository.getItemAuthorAndItemIdMapping();
        Map<String, Long> collect = authors.stream()
                .collect(Collectors.toMap(Pair::getKey,
                        author -> downloads.getOrDefault(author.getValue(), 0L),
                        (a, b) -> a + b));

        return Seq.seq(collect.entrySet())
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5 * limit)
                .map(item -> Pair.of(authorsService.getAuthorLocalization(item.getKey()), item.getValue()))
                .filter(item -> !item.getKey().getSurname(Locale.ENGLISH).toLowerCase().contains("litnarovych"))
                .distinct(Pair::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }


    public Integer getViewsForItem(Integer itemId) {
        return dsl.select(STATISTICS.itemId, DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(STATISTICS.itemId.equal(itemId).and(STATISTICS.sequenceId.lessThan(0)))
                .groupBy(STATISTICS.itemId)
                .fetchOptional(DSL.sum(STATISTICS.viewCount))
                .map(BigDecimal::intValue)
                .orElse(0);
    }

    public Integer getDownloadsForItem(Integer itemId) {
        return dsl.select(STATISTICS.itemId, DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(STATISTICS.itemId.equal(itemId).and(STATISTICS.sequenceId.greaterOrEqual(0)))
                .groupBy(STATISTICS.itemId)
                .fetchOptional(DSL.sum(STATISTICS.viewCount))
                .map(BigDecimal::intValue)
                .orElse(0);
    }

    public void updateItemViews(HttpServletRequest request, Integer itemId) {
        String countryCode = geoIpService.getCountryCode(request);

        dsl.insertInto(STATISTICS)
                .set(STATISTICS.itemId, itemId)
                .set(STATISTICS.sequenceId, -1)
                .set(STATISTICS.countryCode, countryCode)
                .set(STATISTICS.viewCount, 1)
                .onDuplicateKeyUpdate()
                .set(STATISTICS.viewCount, STATISTICS.viewCount.plus(1))
                .execute();
    }

    public void updateItemDownloads(HttpServletRequest request, Integer itemId) {
        String countryCode = geoIpService.getCountryCode(request);

        dsl.insertInto(STATISTICS)
                .set(STATISTICS.itemId, itemId)
                .set(STATISTICS.sequenceId, 1)
                .set(STATISTICS.countryCode, countryCode)
                .set(STATISTICS.viewCount, 1)
                .onDuplicateKeyUpdate()
                .set(STATISTICS.viewCount, STATISTICS.viewCount.plus(1))
                .execute();
    }

    public Map<String, Integer> getItemViewsByCountry(Integer itemId) {
        return dsl.select(STATISTICS.countryCode, DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(STATISTICS.itemId.eq(itemId).and(STATISTICS.sequenceId.lessThan(0)))
                .groupBy(STATISTICS.countryCode)
                .fetch()
                .stream()
                .collect(Collectors.toMap(entry -> entry.get(STATISTICS.countryCode), entry -> entry.get(DSL.sum(STATISTICS.viewCount)).intValue()));
    }

    public Map<String, Integer> getItemDownloadsByCountry(Integer itemId) {
        return dsl.select(STATISTICS.countryCode, DSL.sum(STATISTICS.viewCount))
                .from(STATISTICS)
                .where(STATISTICS.itemId.eq(itemId).and(STATISTICS.sequenceId.greaterOrEqual(0)))
                .groupBy(STATISTICS.countryCode)
                .fetch()
                .stream()
                .collect(Collectors.toMap(entry -> entry.get(STATISTICS.countryCode), entry -> entry.get(DSL.sum(STATISTICS.viewCount)).intValue()));
    }
}

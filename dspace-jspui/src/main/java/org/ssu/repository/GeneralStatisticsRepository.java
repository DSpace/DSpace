package org.ssu.repository;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.ssu.entity.statistics.GeneralStatistics;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeneralStatisticsRepository {
    private static final org.ssu.entity.jooq.GeneralStatistics GENERAL_STATISTICS = org.ssu.entity.jooq.GeneralStatistics.TABLE;

    @Resource
    private DSLContext dsl;

    public List<GeneralStatistics> findAll() {
        return dsl.select(GENERAL_STATISTICS.asterisk())
                .from(GENERAL_STATISTICS)
                .fetch()
                .stream()
                .map(item -> new GeneralStatistics.Builder()
                        .withYear(item.get(GENERAL_STATISTICS.year))
                        .withMonth(item.get(GENERAL_STATISTICS.month))
                        .withDownloadsCount(item.get(GENERAL_STATISTICS.downloadsCount))
                        .withViewsCount(item.get(GENERAL_STATISTICS.viewCount))
                        .build()
                ).collect(Collectors.toList());
    }

    public void save(GeneralStatistics generalStatistics) {
        dsl.insertInto(GENERAL_STATISTICS)
                .set(GENERAL_STATISTICS.year, generalStatistics.getYear())
                .set(GENERAL_STATISTICS.month, generalStatistics.getMonth())
                .set(GENERAL_STATISTICS.viewCount, generalStatistics.getViewsCount())
                .set(GENERAL_STATISTICS.downloadsCount, generalStatistics.getDownloadsCount())
                .onDuplicateKeyIgnore()
                .execute();
    }
}
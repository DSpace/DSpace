/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ExactImportRecords;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.datamodel.RecordsCount;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.components.QuerySource;

/**
 * Implements a data source for querying multiple external data sources in parallel
 *
 * optional Affiliation information are not part of the API request.
 *
 * @author Johanna Staudinger (johanna.staudinger@uni-bamberg.de)
 *
 */
public class MultipleParallelImportMetadataSourceServiceImpl implements QuerySource {
    private final List<QuerySource> innerProviders;
    private final ExecutorService executorService;

    private final String sourceName;
    public MultipleParallelImportMetadataSourceServiceImpl(List<QuerySource> innerProviders, String sourceName) {
        super();
        this.innerProviders = innerProviders;
        this.executorService = Executors.newFixedThreadPool(innerProviders.size());
        this.sourceName = sourceName;
    }

    @Override
    public String getImportSource() {
        return sourceName;
    }

    @Override
    public ImportRecord getRecord(String recordId) throws MetadataSourceException {
        List<Future<ImportRecord>> futureList = new ArrayList<>();
        ImportRecord result = null;
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.getRecord(recordId)));
        }
        for (Future<ImportRecord> future: futureList) {
            try {
                ImportRecord importRecord = future.get();
                if (!Objects.isNull(importRecord)) {
                    result = importRecord;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return getRecordsCountResult(query).getCount();
    }

    @Override
    public RecordsCount getRecordsCountResult(String query) throws MetadataSourceException {
        List<Future<RecordsCount>> futureList = new ArrayList<>();
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.getRecordsCountResult(query)));
        }
        return preferExactCount(futureList);
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return getRecordsCountResult(query).getCount();
    }

    @Override
    public RecordsCount getRecordsCountResult(Query query) throws MetadataSourceException {
        List<Future<RecordsCount>> futureList = new ArrayList<>();
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.getRecordsCountResult(query)));
        }
        return preferExactCount(futureList);
    }


    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        List<Future<Collection<ImportRecord>>> futureList = new ArrayList<>();
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.getRecords(query, start, count)));
        }
        return preferExact(futureList);
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        List<Future<Collection<ImportRecord>>> futureList = new ArrayList<>();
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.getRecords(query)));
        }
        return preferExact(futureList);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This method is not implemented for multiple external data sources");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        List<Future<Collection<ImportRecord>>> futureList = new ArrayList<>();
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.findMatchingRecords(query)));
        }
        return preferExact(futureList);
    }


    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        List<Future<Collection<ImportRecord>>> futureList = new ArrayList<>();
        for (QuerySource innerProvider : innerProviders) {
            futureList.add(executorService.submit(() -> innerProvider.findMatchingRecords(item)));
        }
        return preferExact(futureList);
    }

    /**
     * If any provider returned {@link ExactImportRecords}, only those results are kept.
     */
    private Collection<ImportRecord> preferExact(List<Future<Collection<ImportRecord>>> futureList) {
        List<Collection<ImportRecord>> results = new ArrayList<>();
        for (Future<Collection<ImportRecord>> future : futureList) {
            try {
                Collection<ImportRecord> importRecords = future.get();
                results.add(Objects.isNull(importRecords) ? Collections.emptyList() : importRecords);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        boolean anyExact = results.stream().anyMatch(ExactImportRecords.class::isInstance);
        List<ImportRecord> merged = new ArrayList<>();
        for (Collection<ImportRecord> importRecords : results) {
            if (!anyExact || importRecords instanceof ExactImportRecords) {
                merged.addAll(importRecords);
            }
        }
        return merged;
    }

    /**
     * If any provider returned an exact count, only exact counts are summed.
     */
    private RecordsCount preferExactCount(List<Future<RecordsCount>> futureList) {
        List<RecordsCount> results = new ArrayList<>();
        for (Future<RecordsCount> future : futureList) {
            try {
                RecordsCount count = future.get();
                if (count != null) {
                    results.add(count);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        boolean anyExact = results.stream().anyMatch(RecordsCount::isExact);
        int total = 0;
        for (RecordsCount count : results) {
            if (!anyExact || count.isExact()) {
                total += count.getCount();
            }
        }
        return anyExact ? RecordsCount.exact(total) : RecordsCount.query(total);
    }
}

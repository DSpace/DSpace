/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.lyncode.xoai.dataprovider.core.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.core.ListItemsResults;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.xoai.data.DSpaceSolrItem;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.SolrSearchEmptyException;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceItemSolrRepository extends DSpaceItemRepository {
    private static final Logger log = LogManager.getLogger(DSpaceItemSolrRepository.class);
    private final SolrClient server;
    private final SolrQueryResolver solrQueryResolver;
    private final ResumptionCursor cursor;

    public DSpaceItemSolrRepository(SolrClient server, CollectionsService collectionsService,
                                    HandleResolver handleResolver, SolrQueryResolver solrQueryResolver,
                                    ResumptionCursor cursor) {
        super(collectionsService, handleResolver);
        this.server = server;
        this.solrQueryResolver = solrQueryResolver;
        this.cursor = cursor;
    }

    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException {
        if (identifier == null) {
            throw new IdDoesNotExistException();
        }
        String[] parts = identifier.split(Pattern.quote(":"));
        if (parts.length == 3) {
            try {
                SolrQuery params = new SolrQuery("item.handle:" + parts[2]);
                return new DSpaceSolrItem(DSpaceSolrSearch.querySingle(server, params));
            } catch (SolrSearchEmptyException | IOException ex) {
                throw new IdDoesNotExistException(ex);
            }
        }
        throw new IdDoesNotExistException();
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length) {
        try {
            QueryResult queryResult = retrieveItems(filters, offset, length);
            // transform results list from a list of Items to a list of ItemIdentifiers
            List<ItemIdentifier> identifierList =
                newArrayList(transform(queryResult.getResults(), new Function<Item, ItemIdentifier>() {
                    @Override
                    public ItemIdentifier apply(Item elem) {
                        return elem;
                    }
                }));
            return new ListItemIdentifiersResult(queryResult.hasMore(), identifierList, queryResult.getTotal());
        } catch (DSpaceSolrException | IOException ex) {
            log.error(ex.getMessage(), ex);
            return new ListItemIdentifiersResult(false, new ArrayList<>());
        }
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length) {
        try {
            QueryResult queryResult = retrieveItems(filters, offset, length);
            return new ListItemsResults(queryResult.hasMore(), queryResult.getResults(), queryResult.getTotal());
        } catch (DSpaceSolrException | IOException ex) {
            log.error(ex.getMessage(), ex);
            return new ListItemsResults(false, new ArrayList<>());
        }
    }

    private QueryResult retrieveItems(List<ScopedFilter> filters, int offset, int length)
            throws DSpaceSolrException, IOException {
        List<Item> list = new ArrayList<>();
        String query = solrQueryResolver.buildQuery(filters);
        // Excluding the records already served from the query itself lets Solr seek to this page, instead of
        // walking a priority queue of offset + length entries to throw away all but the last of them.
        // The bound is only meaningful because DSpaceSolrSearch sorts on that very field, ascending.
        // Without a cursor -- first page, or a token issued before they existed -- the offset is skipped as it was.
        boolean seekable = !cursor.isEmpty();
        if (seekable) {
            // Generates: item.id:{cursorValue TO *]
            TermRangeQuery rangeQuery = TermRangeQuery.newStringRange("item.id", cursor.valueOf(), null, false, true);
            query += " AND " + rangeQuery.toString();
        }
        SolrQuery params = new SolrQuery(query)
            .setRows(length)
            .setStart(seekable ? 0 : offset);
        SolrDocumentList solrDocuments = DSpaceSolrSearch.query(server, params);
        for (SolrDocument doc : solrDocuments) {
            list.add(new DSpaceSolrItem(doc));
        }
        if (!solrDocuments.isEmpty()) {
            // move the cursor to the last item from the Solr response
            SolrDocument last = solrDocuments.get(solrDocuments.size() - 1);
            cursor.moveTo((String) last.getFieldValue("item.id"));
        }
        // With the cursor bound in place, numFound is exactly what was left to serve, this page included:
        //   * strictly more than a page means another page
        //   * exactly a page means this one was the last.
        //   * -1 makes the xoai library omit that optional attribute.
        long numFound = solrDocuments.getNumFound();
        return (seekable)
            ? new QueryResult(list, numFound > length, -1)
            : new QueryResult(list, numFound > offset + length, (int) numFound);
    }

    private class QueryResult {
        private List<Item> results;
        private boolean hasMore;
        private int total;

        private QueryResult(List<Item> results, boolean hasMore, int total) {
            this.results = results;
            this.hasMore = hasMore;
            this.total = total;
        }

        private List<Item> getResults() {
            return results;
        }

        private boolean hasMore() {
            return hasMore;
        }

        private int getTotal() {
            return total;
        }
    }

}

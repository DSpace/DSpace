/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.google.common.base.Function;
import com.lyncode.builder.ListBuilder;
import com.lyncode.xoai.dataprovider.core.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.core.ListItemsResults;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.xoai.data.DSpaceSolrItem;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.SolrSearchEmptyException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceItemSolrRepository extends DSpaceItemRepository
{
    private static Logger log = LogManager.getLogger(DSpaceItemSolrRepository.class);
    private SolrServer server;
    private SolrQueryResolver solrQueryResolver;

    public DSpaceItemSolrRepository(SolrServer server, CollectionsService collectionsService, HandleResolver handleResolver, SolrQueryResolver solrQueryResolver)
    {
        super(collectionsService, handleResolver);
        this.server = server;
        this.solrQueryResolver = solrQueryResolver;
    }

    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException {
        if (identifier == null) throw new IdDoesNotExistException();
        String parts[] = identifier.split(Pattern.quote(":"));
        if (parts.length == 3)
        {
            try
            {
                SolrQuery params = new SolrQuery("item.handle:" + parts[2]);
                return new DSpaceSolrItem(DSpaceSolrSearch.querySingle(server, params));
            }
            catch (SolrSearchEmptyException ex)
            {
                throw new IdDoesNotExistException(ex);
            }
        }
        throw new IdDoesNotExistException();
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(
            List<ScopedFilter> filters, int offset, int length)
    {
        try
        {
            QueryResult queryResult = retrieveItems(filters, offset, length);
            List<ItemIdentifier> identifierList = new ListBuilder<Item>()
                    .add(queryResult.getResults())
                    .build(new Function<Item, ItemIdentifier>() {
                        @Override
                        public ItemIdentifier apply(Item elem) {
                            return elem;
                        }
                    });
            return new ListItemIdentifiersResult(queryResult.hasMore(), identifierList, queryResult.getTotal());
        }
        catch (DSpaceSolrException ex)
        {
            log.error(ex.getMessage(), ex);
            return new ListItemIdentifiersResult(false, new ArrayList<ItemIdentifier>());
        }
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length)
    {
        try
        {
            QueryResult queryResult = retrieveItems(filters, offset, length);
            return new ListItemsResults(queryResult.hasMore(), queryResult.getResults(), queryResult.getTotal());
        }
        catch (DSpaceSolrException ex)
        {
            log.error(ex.getMessage(), ex);
            return new ListItemsResults(false, new ArrayList<Item>());
        }
    }

    private QueryResult retrieveItems (List<ScopedFilter> filters, int offset, int length) throws DSpaceSolrException {
        List<Item> list = new ArrayList<Item>();
        SolrQuery params = new SolrQuery(solrQueryResolver.buildQuery(filters))
                .setRows(length)
                .setStart(offset);
        SolrDocumentList solrDocuments = DSpaceSolrSearch.query(server, params);
        for (SolrDocument doc : solrDocuments)
            list.add(new DSpaceSolrItem(doc));
        return new QueryResult(list, (solrDocuments.getNumFound() > offset + length), (int) solrDocuments.getNumFound());
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

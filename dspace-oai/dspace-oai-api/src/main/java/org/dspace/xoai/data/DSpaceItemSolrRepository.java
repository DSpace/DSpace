/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.xoai.filter.DSpaceFilter;
import org.dspace.xoai.filter.SolrFilterResult;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.SolrSearchEmptyException;

import com.lyncode.xoai.dataprovider.core.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.core.ListItemsResults;
import com.lyncode.xoai.dataprovider.data.AbstractItem;
import com.lyncode.xoai.dataprovider.data.AbstractItemIdentifier;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.filter.Filter;
import com.lyncode.xoai.dataprovider.filter.FilterScope;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceItemSolrRepository extends DSpaceItemRepository
{
    private static Logger log = LogManager.getLogger(DSpaceItemSolrRepository.class);

    public DSpaceItemSolrRepository()
    {
    }

    @Override
    public AbstractItem getItem(String identifier)
            throws IdDoesNotExistException
    {
        String parts[] = identifier.split(Pattern.quote(":"));
        if (parts.length == 3)
        {
            try
            {
                SolrQuery params = new SolrQuery("item.handle:" + parts[2]);
                return new DSpaceSolrItem(DSpaceSolrSearch.querySingle(params));
            }
            catch (SolrSearchEmptyException ex)
            {
                throw new IdDoesNotExistException(ex);
            }
        }
        throw new IdDoesNotExistException();
    }

    @Override
    protected ListItemIdentifiersResult getItemIdentifiers(
            List<Filter> filters, int offset, int length)
    {
        List<String> whereCond = new ArrayList<String>();
        for (Filter filter : filters)
        {
            if (filter.getFilter() instanceof DSpaceFilter)
            {
                DSpaceFilter dspaceFilter = (DSpaceFilter) filter.getFilter();
                SolrFilterResult result = dspaceFilter.getQuery();
                if (result.hasResult())
                {
                    if (filter.getScope() == FilterScope.MetadataFormat)
                        whereCond.add("(item.deleted:true OR ("
                                + result.getQuery() + "))");
                    else
                        whereCond.add("(" + result.getQuery() + ")");
                }
            }
        }
        if (whereCond.isEmpty())
            whereCond.add("*:*");
        String where = "(" + StringUtils.join(whereCond.iterator(), ") AND (")
                + ")";
        return this.getIdentifierResult(where, offset, length);
    }

    @Override
    protected ListItemsResults getItems(List<Filter> filters, int offset,
            int length)
    {
        List<String> whereCond = new ArrayList<String>();
        for (Filter filter : filters)
        {
            if (filter.getFilter() instanceof DSpaceFilter)
            {
                DSpaceFilter dspaceFilter = (DSpaceFilter) filter.getFilter();
                SolrFilterResult result = dspaceFilter.getQuery();
                if (result.hasResult())
                {
                    if (filter.getScope() == FilterScope.MetadataFormat)
                        whereCond.add("(item.deleted:true OR ("
                                + result.getQuery() + "))");
                    else
                        whereCond.add("(" + result.getQuery() + ")");
                }
            }
        }
        if (whereCond.isEmpty())
            whereCond.add("*:*");
        String where = "(" + StringUtils.join(whereCond.iterator(), ") AND (")
                + ")";
        return this.getResult(where, offset, length);
    }

    private ListItemsResults getResult(String where, int offset, int length)
    {
        List<AbstractItem> list = new ArrayList<AbstractItem>();
        try
        {
            SolrQuery params = new SolrQuery(where).setRows(length).setStart(
                    offset);
            SolrDocumentList docs = DSpaceSolrSearch.query(params);
            for (SolrDocument doc : docs)
            {
                list.add(new DSpaceSolrItem(doc));
            }
            return new ListItemsResults((docs.getNumFound() > offset + length),
                    list, (int) docs.getNumFound());
        }
        catch (DSpaceSolrException ex)
        {
            log.error(ex.getMessage(), ex);
            return new ListItemsResults(false, list);
        }
    }

    private ListItemIdentifiersResult getIdentifierResult(String where,
            int offset, int length)
    {
        List<AbstractItemIdentifier> list = new ArrayList<AbstractItemIdentifier>();
        try
        {
            SolrQuery params = new SolrQuery(where).setRows(length).setStart(
                    offset);
            boolean hasMore = false;
            SolrDocumentList docs = DSpaceSolrSearch.query(params);
            hasMore = (offset + length) < docs.getNumFound();
            for (SolrDocument doc : docs)
            {
                list.add(new DSpaceSolrItem(doc));
            }
            return new ListItemIdentifiersResult(hasMore, list, (int) docs.getNumFound());
        }
        catch (DSpaceSolrException ex)
        {
            log.error(ex.getMessage(), ex);
            return new ListItemIdentifiersResult(false, list);
        }
    }

}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.utils.DSpace;

/**
 * 
 * @author Andrea Bollini (CILEA)
 *
 */
public class SolrBrowseDAO implements BrowseDAO
{
    public SolrBrowseDAO(Context context)
    {
        this.context = context;
    }

    static private class FacetValueComparator implements Comparator
    {
        @Override
        public int compare(Object o1, Object o2)
        {
            String s1 = "", s2 = "";
            if (o1 instanceof FacetResult && o2 instanceof String)
            {
                FacetResult c = (FacetResult) o1;
                s1 = c.getSortValue();
                s2 = (String) o2;
            }
            else if (o2 instanceof FacetResult && o1 instanceof String)
            {
                FacetResult c = (FacetResult) o2;
                s1 = (String) o1;
                s2 = c.getSortValue();
            }
            // both object are FacetResult so they are already sorted
            return s1.compareTo(s2);
        }
    }

    /** Log4j log */
    private static Logger log = Logger.getLogger(SolrBrowseDAO.class);

    /** The DSpace context */
    private Context context;

    // SQL query related attributes for this class

    /** table(s) to select from */
    private String table = null;

    /** field to look for focus value in */
    private String focusField = null;

    /** value to start browse from in focus field */
    private String focusValue = null;

    /** field to look for value in */
    private String valueField = null;

    /** value to restrict browse to (e.g. author name) */
    private String value = null;

    private String authority = null;

    /** exact or partial matching of the value */
    private boolean valuePartial = false;

    /** the table that defines the mapping for the relevant container */
    private String containerTable = null;

    /**
     * the name of the field which contains the container id (e.g.
     * collection_id)
     */
    private String containerIDField = null;

    /** the database id of the container we are constraining to */
    private int containerID = -1;

    /** the column that we are sorting results by */
    private String orderField = null;

    /** whether to sort results ascending or descending */
    private boolean ascending = true;

    /** the limit of number of results to return */
    private int limit = -1;

    /** the offset of the start point */
    private int offset = 0;

    /** whether to use the equals comparator in value comparisons */
    private boolean equalsComparator = true;

    /** whether this is a distinct browse or not */
    private boolean distinct = false;

    private String facetField;

    // administrative attributes for this class

    DSpace dspace = new DSpace();

    SearchService searcher = dspace.getServiceManager().getServiceByName(
            SearchService.class.getName(), SearchService.class);

    private DiscoverResult sResponse = null;

    private boolean itemsWithdrawn = false;
    private boolean itemsDiscoverable = true;

    private boolean showFrequencies;
    
    private DiscoverResult getSolrResponse() throws BrowseException
    {
        if (sResponse == null)
        {
            DiscoverQuery query = new DiscoverQuery();
            addLocationScopeFilter(query);
            addStatusFilter(query);
            if (distinct)
            {
                DiscoverFacetField dff = new DiscoverFacetField(facetField,
                        DiscoveryConfigurationParameters.TYPE_TEXT, -1,
                        DiscoveryConfigurationParameters.SORT.VALUE);
                query.addFacetField(dff);
                query.setFacetMinCount(1);
                query.setMaxResults(0);
            }
            else
            {
                query.setMaxResults(limit > 0 ? limit : 20);
                if (offset > 0)
                {
                    query.setStart(offset);
                }

                // caution check first authority, value is always present!
                if (authority != null)
                {
                    query.addFilterQueries("{!field f="+facetField + "_authority_filter}"
                            + authority);
                }
                else if (value != null && !valuePartial)
                {
                    query.addFilterQueries("{!field f="+facetField + "_value_filter}" + value);
                }
                else if (valuePartial)
                {
                    query.addFilterQueries("{!field f="+facetField + "_partial}" + value);
                }
                // filter on item to be sure to don't include any other object
                // indexed in the Discovery Search core
                query.addFilterQueries("search.resourcetype:" + Constants.ITEM);
                if (orderField != null)
                {
                    query.setSortField("bi_" + orderField + "_sort",
                            ascending ? SORT_ORDER.asc : SORT_ORDER.desc);
                }
            }
            try
            {
				sResponse = searcher.search(context, query, itemsWithdrawn
						|| !itemsDiscoverable);
            }
            catch (SearchServiceException e)
            {
                throw new BrowseException(e);
            }
        }
        return sResponse;
    }

    private void addStatusFilter(DiscoverQuery query)
    {
        if (itemsWithdrawn)
        {
            query.addFilterQueries("withdrawn:true");
        }
        else if (!itemsDiscoverable)
        {
            query.addFilterQueries("discoverable:false");    
        }
    }

    private void addLocationScopeFilter(DiscoverQuery query)
    {
        if (containerID > 0)
        {
            if (containerIDField.startsWith("collection"))
            {
                query.addFilterQueries("location.coll:" + containerID);
            }
            else if (containerIDField.startsWith("community"))
            {
                query.addFilterQueries("location.comm:" + containerID);
            }
        }
    }

    @Override
    public int doCountQuery() throws BrowseException
    {
        DiscoverResult resp = getSolrResponse();
        int count = 0;
        if (distinct)
        {
            List<FacetResult> facetResults = resp.getFacetResult(facetField);
            count = facetResults.size();
        }
        else
        {
            // we need to cast to int to respect the BrowseDAO contract...
            count = (int) resp.getTotalSearchResults();
            // FIXME null the response cache
            // the BrowseEngine send fake argument to the BrowseDAO for the
            // count...
            sResponse = null;
        }
        return count;
    }

    @Override
    public List doValueQuery() throws BrowseException
    {
        DiscoverResult resp = getSolrResponse();
        List<FacetResult> facet = resp.getFacetResult(facetField);
        int count = doCountQuery();
        int start = offset > 0 ? offset : 0;
        int max = limit > 0 ? limit : 20;
        List<String[]> result = new ArrayList<String[]>();
        if (ascending)
        {
            for (int i = start; i < (start + max) && i < count; i++)
            {
                FacetResult c = facet.get(i);
                String freq = showFrequencies ? String.valueOf(c.getCount())
                        : "";
                result.add(new String[] { c.getDisplayedValue(),
                        c.getAuthorityKey(), freq });
            }
        }
        else
        {
            for (int i = count - start - 1; i >= count - (start + max)
                    && i >= 0; i--)
            {
                FacetResult c = facet.get(i);
                String freq = showFrequencies ? String.valueOf(c.getCount())
                        : "";
                result.add(new String[] { c.getDisplayedValue(),
                        c.getAuthorityKey(), freq });
            }
        }

        return result;
    }

    @Override
    public List doQuery() throws BrowseException
    {
        DiscoverResult resp = getSolrResponse();

        List<BrowseItem> bitems = new ArrayList<BrowseItem>();
        for (DSpaceObject solrDoc : resp.getDspaceObjects())
        {
            // FIXME introduce project, don't retrieve Item immediately when
            // processing the query...
            Item item = (Item) solrDoc;
            BrowseItem bitem = new BrowseItem(context, item.getID(),
                    item.isArchived(), item.isWithdrawn(), item.isDiscoverable());
            bitems.add(bitem);
        }
        return bitems;
    }

    @Override
    public String doMaxQuery(String column, String table, int itemID)
            throws BrowseException
    {
        DiscoverQuery query = new DiscoverQuery();
        query.setQuery("search.resourceid:" + itemID
                + " AND search.resourcetype:" + Constants.ITEM);
        query.setMaxResults(1);
        DiscoverResult resp = null;
        try
        {
            resp = searcher.search(context, query);
        }
        catch (SearchServiceException e)
        {
            throw new BrowseException(e);
        }
        if (resp.getTotalSearchResults() > 0)
        {
            SearchDocument doc = resp.getSearchDocument(
                    resp.getDspaceObjects().get(0)).get(0);
            return (String) doc.getSearchFieldValues(column).get(0);
        }
        return null;
    }

    @Override
    public int doOffsetQuery(String column, String value, boolean isAscending)
            throws BrowseException
    {
        DiscoverQuery query = new DiscoverQuery();
        addLocationScopeFilter(query);
        addStatusFilter(query);
        query.setMaxResults(0);
        query.addFilterQueries("search.resourcetype:" + Constants.ITEM);
        if (isAscending)
        {
            query.setQuery("bi_"+column + "_sort" + ": [* TO \"" + value + "\"]");
        }
        else
        {
            query.setQuery("bi_" + column + "_sort" + ": [\"" + value + "\" TO *]");
        }
        DiscoverResult resp = null;
        try
        {
            resp = searcher.search(context, query);
        }
        catch (SearchServiceException e)
        {
            throw new BrowseException(e);
        }
        return (int) resp.getTotalSearchResults();
    }

    @Override
    public int doDistinctOffsetQuery(String column, String value,
            boolean isAscending) throws BrowseException
    {
        DiscoverResult resp = getSolrResponse();
        List<FacetResult> facets = resp.getFacetResult(facetField);
        Comparator comparator = new SolrBrowseDAO.FacetValueComparator();
        Collections.sort(facets, comparator);
        int x = Collections.binarySearch(facets, value, comparator);
        int ascValue = (x >= 0) ? x : -(x + 1);
        if (isAscending)
        {
            return ascValue;
        }
        else
        {
            return doCountQuery() - ascValue;
        }
    }
    
    @Override
    public boolean isEnableBrowseFrequencies()
    {
        return showFrequencies;
    }
    
    @Override
    public void setEnableBrowseFrequencies(boolean enableBrowseFrequencies)
    {
        showFrequencies = enableBrowseFrequencies;        
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getContainerID()
     */
    public int getContainerID()
    {
        return containerID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getContainerIDField()
     */
    public String getContainerIDField()
    {
        return containerIDField;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getContainerTable()
     */
    public String getContainerTable()
    {
        return containerTable;
    }

    // FIXME is this in use?
    public String[] getCountValues()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getFocusField()
     */
    public String getJumpToField()
    {
        return focusField;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getFocusValue()
     */
    public String getJumpToValue()
    {
        return focusValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getLimit()
     */
    public int getLimit()
    {
        return limit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getOffset()
     */
    public int getOffset()
    {
        return offset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getOrderField()
     */
    public String getOrderField()
    {
        return orderField;
    }

    // is this in use?
    public String[] getSelectValues()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getTable()
     */
    public String getTable()
    {
        return table;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getValue()
     */
    public String getFilterValue()
    {
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#getValueField()
     */
    public String getFilterValueField()
    {
        return valueField;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#isAscending()
     */
    public boolean isAscending()
    {
        return ascending;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#isDistinct()
     */
    public boolean isDistinct()
    {
        return this.distinct;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setAscending(boolean)
     */
    public void setAscending(boolean ascending)
    {
        this.ascending = ascending;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setContainerID(int)
     */
    public void setContainerID(int containerID)
    {
        this.containerID = containerID;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setContainerIDField(java.lang.String)
     */
    public void setContainerIDField(String containerIDField)
    {
        this.containerIDField = containerIDField;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setContainerTable(java.lang.String)
     */
    public void setContainerTable(String containerTable)
    {
        this.containerTable = containerTable;

    }

    // is this in use?
    public void setCountValues(String[] fields)
    {
        // this.countValues = fields;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setDistinct(boolean)
     */
    public void setDistinct(boolean bool)
    {
        this.distinct = bool;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setEqualsComparator(boolean)
     */
    public void setEqualsComparator(boolean equalsComparator)
    {
        this.equalsComparator = equalsComparator;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setFocusField(java.lang.String)
     */
    public void setJumpToField(String focusField)
    {
        this.focusField = focusField;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setFocusValue(java.lang.String)
     */
    public void setJumpToValue(String focusValue)
    {
        this.focusValue = focusValue;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setLimit(int)
     */
    public void setLimit(int limit)
    {
        this.limit = limit;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setOffset(int)
     */
    public void setOffset(int offset)
    {
        this.offset = offset;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setOrderField(java.lang.String)
     */
    public void setOrderField(String orderField)
    {
        this.orderField = orderField;

    }

    // is this in use?
    public void setSelectValues(String[] selectValues)
    {
        // this.selectValues = selectValues;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setTable(java.lang.String)
     */
    public void setTable(String table)
    {
        if (table.equals(BrowseIndex.getWithdrawnBrowseIndex().getTableName()))
        {
            itemsWithdrawn = true;
        }
        else if (table.equals(BrowseIndex.getPrivateBrowseIndex().getTableName()))
        {
            itemsDiscoverable = false;
        }
        facetField = table;
    }

    public void setFilterMappingTables(String tableDis, String tableMap)
    {
        if (tableDis != null)
        {
            this.facetField = tableDis;
        }
        // this.fields = tableDis;
        // this.tableMap = tableMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setValue(java.lang.String)
     */
    public void setFilterValue(String value)
    {
        this.value = value;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setFilterValuePartial(boolean)
     */
    public void setFilterValuePartial(boolean part)
    {
        this.valuePartial = part;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#setValueField(java.lang.String)
     */
    public void setFilterValueField(String valueField)
    {
        this.valueField = valueField;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseDAO#useEqualsComparator()
     */
    public boolean useEqualsComparator()
    {
        return equalsComparator;
    }

    @Override
    public String getAuthorityValue()
    {
        return authority;
    }

    @Override
    public void setAuthorityValue(String value)
    {
        this.authority = value;
    }
}

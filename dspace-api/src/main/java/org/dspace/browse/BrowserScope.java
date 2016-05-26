/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;

/**
 * A class which represents the initial request to the browse system.
 * When passed into the BrowseEngine, this will cause the creation
 * of a BrowseInfo object
 *
 * @author Richard Jones
 *
 */
public class BrowserScope
{
    /** the DSpace context */
    private Context context;

    /** the current browse index */
    private BrowseIndex browseIndex;

    /** the order in which to display results */
    private String order;

    /** the field upon which to sort */
    private int sortBy;

    /** the value to restrict the browse to */
    private String filterValue;

    /** exact or partial matching of the value */
    private boolean filterValuePartial = false;

    /** the language of the value to restrict the browse to */
    private String filterValueLang;

    /** the item id focus of the browse */
    private int jumpItemId = -1;

    /** the string value to start with */
    private String startsWith;

    /** the number of results per page to display */
    private int resultsPerPage = 20;

    /** the Collection to which to restrict */
    private Collection collection;

    /** the Community to which to restrict */
    private Community community;

    /** the sort option being used */
    private SortOption sortOption;

    /** the value upon which to focus */
    private String jumpValue;

    /** the language of the value upon which to focus */
    private String jumpValueLang;

    /** the browse level */
    private int level = 0;

    /** the number of authors to display in the results */
    private int etAl = 0;

    /** the number of items to offset into the result ie. 0 = 1st record */
    private int offset = 0;

    private String authority = null;

    /**
     * Construct a new BrowserScope using the given Context
     *
     * @param context   the DSpace Context
     */
    public BrowserScope(Context context)
    {
        this.context = context;
    }

    /**
     * Set the DSpaceObject that is the container for this browse.  If this
     * is not of type Collection or Community, this method will throw an
     * exception
     *
     * @param dso       the container object; a Community or Collection
     * @throws BrowseException if browse error
     */
    public void setBrowseContainer(DSpaceObject dso)
        throws BrowseException
    {
        if (dso instanceof Collection)
        {
            this.collection = (Collection) dso;
        }
        else if (dso instanceof Community)
        {
            this.community = (Community) dso;
        }
        else
        {
            throw new BrowseException("The container must be a community or a collection");
        }
    }

    /**
     * Obtain a DSpaceObject that represents the container object.  This will be
     * a Community or a Collection
     *
     * @return  A DSpaceObject representing a Community or a Collection
     */
    public DSpaceObject getBrowseContainer()
    {
        if (this.collection != null)
        {
            return this.collection;
        }
        if (this.community != null)
        {
            return this.community;
        }
        return null;
    }

    /**
     * @param level     the browse level
     */
    public void setBrowseLevel(int level)
    {
        this.level = level;
    }

    /**
     * @return  the browse level
     */
    public int getBrowseLevel()
    {
        return this.level;
    }

    /**
     * @return true if top level browse, false if not
     */
    public boolean isTopLevel()
    {
        if (this.level == 0)
        {
            return true;
        }
        return false;
    }

    /**
     * @return  true if second level browse, false if not
     */
    public boolean isSecondLevel()
    {
        if (this.level == 1)
        {
            return true;
        }
        return false;
    }

    /**
     * @return Returns the browseIndex.
     */
    public BrowseIndex getBrowseIndex()
    {
        return browseIndex;
    }

    /**
     * @param browseIndex The browseIndex to set.
     * @throws BrowseException if error
     */
    public void setBrowseIndex(BrowseIndex browseIndex)
        throws BrowseException
    {
        this.browseIndex = browseIndex;
    }

    /**
     * @return Returns the author limit.
     */
    public int getEtAl()
    {
        return etAl;
    }

    /**
     * @param etAl the author limit
     */
    public void setEtAl(int etAl)
    {
        this.etAl = etAl;
    }

    /**
     * @return Returns the collection.
     */
    public Collection getCollection()
    {
        return collection;
    }

    /**
     * @param collection The collection to set.
     */
    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }

    /**
     * @return Returns the community.
     */
    public Community getCommunity()
    {
        return community;
    }

    /**
     * @param community The community to set.
     */
    public void setCommunity(Community community)
    {
        this.community = community;
    }

    /**
     * @return Returns the context.
     */
    public Context getContext()
    {
        return context;
    }

    /**
     * @param context The context to set.
     */
    public void setContext(Context context)
    {
        this.context = context;
    }

    /**
     * @return Returns the focus.
     */
    public int getJumpToItem()
    {
        return jumpItemId;
    }

    /**
     * @param itemId The focus to set.
     */
    public void setJumpToItem(int itemId)
    {
        this.jumpItemId = itemId;
    }

    /**
     * @return  the value to focus on
     */
    public String getJumpToValue()
    {
        return jumpValue;
    }

    /**
     * @param value     the value to focus on
     */
    public void setJumpToValue(String value)
    {
        this.jumpValue = value;
    }

    /**
     * @return  the language of the value to focus on
     */
    public String getJumpToValueLang()
    {
        return jumpValueLang;
    }

    /**
     * @param valueLang     the language of the value to focus on
     */
    public void setJumpToValueLang(String valueLang)
    {
        this.jumpValueLang = valueLang;
    }

    /**
     * @return Returns the order.
     */
    public String getOrder()
    {
        if (order != null)
        {
            return order;
        }

        BrowseIndex bi = getBrowseIndex();
        if (bi != null)
        {
            return bi.getDefaultOrder();
        }

        return SortOption.ASCENDING;
    }

    /**
     * @param order The order to set.
     */
    public void setOrder(String order)
    {
        if (order == null)
        {
            this.order = null;
        }
        else if (SortOption.ASCENDING.equalsIgnoreCase(order))
        {
            this.order = SortOption.ASCENDING;
        }
        else if (SortOption.DESCENDING.equalsIgnoreCase(order))
        {
            this.order = SortOption.DESCENDING;
        }
    }

    /**
     * @return Returns the resultsPerPage.
     */
    public int getResultsPerPage()
    {
        return resultsPerPage;
    }

    /**
     * @param resultsPerPage The resultsPerPage to set.
     */
    public void setResultsPerPage(int resultsPerPage)
    {
        if (resultsPerPage > -1 || (browseIndex != null && browseIndex.isTagCloudEnabled()))
        {
            this.resultsPerPage = resultsPerPage;
        }
    }

    /**
     * @return Returns the sortBy.
     */
    public int getSortBy()
    {
        return sortBy;
    }

    /**
     * @param sortBy The sortBy to set.
     * @throws BrowseException if error
     */
    public void setSortBy(int sortBy)
        throws BrowseException
    {
        this.sortBy = sortBy;
    }

    /**
     * @return returns the offset for the browse
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * @param offset  the offset to use for this scope
     */
    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    /**
     * Obtain the sort option
     *
     * @return  the sort option
     * @throws BrowseException if browse error
     */
    public SortOption getSortOption()
        throws BrowseException
    {
        try
        {
            // If a sortOption hasn't been set, work out the default, providing we have an index
            if (sortOption == null && browseIndex != null)
            {
                // If a sorting hasn't been specified, and it's a metadata browse
                if (sortBy <= 0 && browseIndex.isMetadataIndex())
                {
                    // Create a dummy sortOption for the metadata sort
                    String dataType = browseIndex.getDataType();
                    String type = ("date".equals(dataType) ? "date" : "text");
                    sortOption = new SortOption(0, browseIndex.getName(), browseIndex.getMetadata(0), type);
                }
                else
                {
                    // If a sorting hasn't been specified
                    if (sortBy <= 0)
                    {
                        // Get the sort option from the index
                        sortOption = browseIndex.getSortOption();

                        if (sortOption == null)
                        {
                            // No sort option, so default to the first one defined in the config
                            for (SortOption so : SortOption.getSortOptions())
                            {
                                sortOption = so;
                                break;
                            }
                        }
                    }
                    else
                    {
                        // A sorting has been specified, so get it from the configured sort columns
                        for (SortOption so : SortOption.getSortOptions())
                        {
                            if (so.getNumber() == sortBy)
                            {
                                sortOption = so;
                            }
                        }
                    }
                }
            }

            return sortOption;
        }
        catch (SortException se)
        {
            throw new BrowseException("Error in SortOptions", se);
        }
    }

    /**
     * @return Returns the startsWith.
     */
    public String getStartsWith()
    {
        return startsWith;
    }

    /**
     * @param startsWith The startsWith to set.
     */
    public void setStartsWith(String startsWith)
    {
        this.startsWith = startsWith;
    }

    /**
     * Used for second-level item browses,
     * to only display items that match the value
     * @return Returns the value.
     */
    public String getFilterValue()
    {
        return filterValue;
    }

    /**
     * Used for second-level item browses,
     * to only display items that match the value
     * @param value The value to set.
     */
    public void setFilterValue(String value)
    {
        this.filterValue = value;
    }

    /**
     * Should the filter value be treated as partial, or exact
     * @return true if partial, false if exact
     */
    public boolean getFilterValuePartial()
    {
        return filterValuePartial;
    }

    /**
     * Should the filter value be treated as partial, or exact
     * @param filterValuePartial true if partial, false if exact
     */
    public void setFilterValuePartial(boolean filterValuePartial)
    {
        this.filterValuePartial = filterValuePartial;
    }

    /**
     * @return Returns the language.
     */
    public String getFilterValueLang()
    {
        return filterValueLang;
    }

    /**
     * @param lang The language to set.
     */
    public void setFilterValueLang(String lang)
    {
        this.filterValueLang = lang;
    }

    /**
     * @return  true if in community, false if not
     */
    public boolean inCommunity()
    {
        if (this.community != null)
        {
            return true;
        }
        return false;
    }

    /**
     * @return  true if in collection, false if not
     */
    public boolean inCollection()
    {
        if (this.collection != null)
        {
            return true;
        }
        return false;
    }

    /**
     * @return  true if ascending, false if not - or not set
     */
    public boolean isAscending()
    {
        if (SortOption.ASCENDING.equalsIgnoreCase(order))
        {
            return true;
        }

        if (SortOption.DESCENDING.equalsIgnoreCase(order))
        {
            return false;
        }

        BrowseIndex bi = getBrowseIndex();

        if (bi != null && SortOption.DESCENDING.equalsIgnoreCase(bi.getDefaultOrder()))
        {
            return false;
        }

        return true;
    }

    /**
     * @return  true if has value, false if not
     */
    public boolean hasFilterValue()
    {
        if (filterValue == null || "".equals(filterValue))
        {
            return false;
        }
        return true;
    }

    /**
     * @return  true if has item focus, false if not
     */
    public boolean hasJumpToItem()
    {
        if (jumpItemId == -1)
        {
            return false;
        }
        return true;
    }

    /**
     * @return  true if has value focus, false if not
     */
    public boolean hasJumpToValue()
    {
        if (this.jumpValue != null)
        {
            return true;
        }
        return false;
    }

    /**
     * @return  true if has starts with value, false if not
     */
    public boolean hasStartsWith()
    {
        if (this.startsWith != null)
        {
            return true;
        }
        return false;
    }

    public String getAuthorityValue() {
        return authority;
    }

    public void setAuthorityValue(String value) {
        authority = value;
    }
}

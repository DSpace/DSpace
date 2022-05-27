/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter.service;

import java.util.List;
import java.util.Map;

import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * MediaFilterManager is the class that invokes the media/format filters over the
 * repository's content. A few command line flags affect the operation of the
 * MFM: -v verbose outputs all extracted text to STDOUT; -f force forces all
 * bitstreams to be processed, even if they have been before; -n noindex does not
 * recreate index after processing bitstreams; -i [identifier] limits processing
 * scope to a community, collection or item; and -m [max] limits processing to a
 * maximum number of items.
 */
public interface MediaFilterService {

    //separator in filterFormats Map between a filter class name and a plugin name,
    //for MediaFilters which extend SelfNamedPlugin (\034 is "file separator" char)
    public static final String FILTER_PLUGIN_SEPARATOR = "\034";


    public void applyFiltersAllItems(Context context) throws Exception;

    public void applyFiltersCommunity(Context context, Community community)
        throws Exception;

    public void applyFiltersCollection(Context context, Collection collection)
        throws Exception;

    public void applyFiltersItem(Context c, Item item) throws Exception;


    /**
     * Iterate through the item's bitstreams in the ORIGINAL bundle, applying
     * filters if possible.
     *
     * @param context context
     * @param myItem  item
     * @return true if any bitstreams processed,
     * false if none
     * @throws Exception if error
     */
    public boolean filterItem(Context context, Item myItem) throws Exception;

    /**
     * Attempt to filter a bitstream.
     *
     * An exception will be thrown if the media filter class cannot be
     * instantiated.  Exceptions from filtering will be logged to STDOUT and
     * swallowed.
     *
     * @param c           context
     * @param myItem      item
     * @param myBitstream bitstream
     * @return true if bitstream processed,
     * false if no applicable filter or already processed
     * @throws Exception if error
     */
    public boolean filterBitstream(Context c, Item myItem, Bitstream myBitstream) throws Exception;

    /**
     * A utility class that calls the virtual methods
     * from the current MediaFilter class.
     * It scans the bitstreams in an item, and decides if a bitstream has
     * already been filtered, and if not or if overWrite is set, invokes the
     * filter.
     *
     * @param context      context
     * @param item         item containing bitstream to process
     * @param source       source bitstream to process
     * @param formatFilter FormatFilter to perform filtering
     * @return true if new rendition is created, false if rendition already
     * exists and overWrite is not set
     * @throws Exception if error occurs
     */
    public boolean processBitstream(Context context, Item item, Bitstream source, FormatFilter formatFilter)
        throws Exception;

    /**
     * Return the item that is currently being processed/filtered
     * by the MediaFilterManager.
     * <p>
     * This allows FormatFilters to retrieve the Item object
     * in case they need access to item-level information for their format
     * transformations/conversions.
     *
     * @return current Item being processed by MediaFilterManager
     */
    public Item getCurrentItem();

    /**
     * Check whether or not to skip processing the given identifier.
     *
     * @param identifier identifier (handle) of a community, collection or item
     * @return true if this community, collection or item should be skipped
     * during processing.  Otherwise, return false.
     */
    public boolean inSkipList(String identifier);

    public void setVerbose(boolean isVerbose);

    public void setQuiet(boolean isQuiet);

    public void setForce(boolean isForce);

    public void setMax2Process(int max2Process);

    public void setFilterClasses(List<FormatFilter> filterClasses);

    public void setSkipList(List<String> skipList);

    public void setFilterFormats(Map<String, List<String>> filterFormats);

    /**
     * Set the log handler used in the DSpace scripts and processes framework
     * @param handler
     */
    public void setLogHandler(DSpaceRunnableHandler handler);
}

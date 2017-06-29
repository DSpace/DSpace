/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.*;

/**
 * MediaFilterManager is the class that invokes the media/format filters over the
 * repository's content. A few command line flags affect the operation of the
 * MFM: -v verbose outputs all extracted text to STDOUT; -f force forces all
 * bitstreams to be processed, even if they have been before; -n noindex does not
 * recreate index after processing bitstreams; -i [identifier] limits processing 
 * scope to a community, collection or item; and -m [max] limits processing to a
 * maximum number of items.
 */
public class MediaFilterServiceImpl implements MediaFilterService, InitializingBean
{
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;

    protected int max2Process = Integer.MAX_VALUE;  // maximum number items to process
    
    protected int processed = 0;   // number items processed
    
    protected Item currentItem = null;   // current item being processed

    protected List<FormatFilter> filterClasses = null;
    
    protected Map<String, List<String>> filterFormats = new HashMap<>();

    protected List<String> skipList = null; //list of identifiers to skip during processing

    protected final List<String> publicFiltersClasses = new ArrayList<>();

    protected boolean isVerbose = false;
    protected boolean isQuiet = false;
    protected boolean isForce = false; // default to not forced

    protected MediaFilterServiceImpl()
    {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String[] publicPermissionFilters = configurationService.getArrayProperty("filter.org.dspace.app.mediafilter.publicPermission");

        if(publicPermissionFilters != null) {
            for(String filter : publicPermissionFilters) {
                publicFiltersClasses.add(filter.trim());
            }
        }
    }

    @Override
    public void applyFiltersAllItems(Context context) throws Exception
    {
        if(skipList!=null)
        {    
            //if a skip-list exists, we need to filter community-by-community
            //so we can respect what is in the skip-list
            List<Community> topLevelCommunities = communityService.findAllTop(context);

            for (Community topLevelCommunity : topLevelCommunities) {
                applyFiltersCommunity(context, topLevelCommunity);
            }
        }
        else 
        {
            //otherwise, just find every item and process
            Iterator<Item> itemIterator = itemService.findAll(context);
            while (itemIterator.hasNext() && processed < max2Process)
            {
                applyFiltersItem(context, itemIterator.next());
            }
        }
    }

    @Override
    public void applyFiltersCommunity(Context context, Community community)
                                             throws Exception
    {   //only apply filters if community not in skip-list
        if(!inSkipList(community.getHandle()))
        {    
           	List<Community> subcommunities = community.getSubcommunities();
            for (Community subcommunity : subcommunities) {
                applyFiltersCommunity(context, subcommunity);
            }
           	
           	List<Collection> collections = community.getCollections();
            for (Collection collection : collections) {
                applyFiltersCollection(context, collection);
            }
        }
    }

    @Override
    public void applyFiltersCollection(Context context, Collection collection)
                                              throws Exception
    {
        //only apply filters if collection not in skip-list
        if(!inSkipList(collection.getHandle()))
        {
            Iterator<Item> itemIterator = itemService.findAllByCollection(context, collection);
            while (itemIterator.hasNext() && processed < max2Process)
            {
                applyFiltersItem(context, itemIterator.next());
            }
        }
    }
       
    @Override
    public void applyFiltersItem(Context c, Item item) throws Exception
    {
        //only apply filters if item not in skip-list
        if(!inSkipList(item.getHandle()))
        {
    	  //cache this item in MediaFilterManager
    	  //so it can be accessed by MediaFilters as necessary
    	  currentItem = item;
    	
          if (filterItem(c, item))
          {
              // increment processed count
              ++processed;
          }
          // clear item objects from context cache and internal cache
          c.uncacheEntity(currentItem);
          currentItem = null;
        }  
    }

    @Override
    public boolean filterItem(Context context, Item myItem) throws Exception
    {
        // get 'original' bundles
        List<Bundle> myBundles = itemService.getBundles(myItem, "ORIGINAL");
        boolean done = false;
        for (Bundle myBundle : myBundles) {
            // now look at all of the bitstreams
            List<Bitstream> myBitstreams = myBundle.getBitstreams();

            for (Bitstream myBitstream : myBitstreams) {
                done |= filterBitstream(context, myItem, myBitstream);
            }
        }
        return done;
    }

    @Override
    public boolean filterBitstream(Context context, Item myItem,
            Bitstream myBitstream) throws Exception
    {
    	boolean filtered = false;
    	
    	// iterate through filter classes. A single format may be actioned
    	// by more than one filter
        for (FormatFilter filterClass : filterClasses) {
            //List fmts = (List)filterFormats.get(filterClasses[i].getClass().getName());
            String pluginName = null;

            //if this filter class is a SelfNamedPlugin,
            //its list of supported formats is different for
            //differently named "plugin"
            if (SelfNamedPlugin.class.isAssignableFrom(filterClass.getClass())) {
                //get plugin instance name for this media filter
                pluginName = ((SelfNamedPlugin) filterClass).getPluginInstanceName();
            }

            //Get list of supported formats for the filter (and possibly named plugin)
            //For SelfNamedPlugins, map key is:
            //  <class-name><separator><plugin-name>
            //For other MediaFilters, map key is just:
            //  <class-name>
            List<String> fmts = filterFormats.get(filterClass.getClass().getName() +
                    (pluginName != null ? FILTER_PLUGIN_SEPARATOR + pluginName : ""));

            if (fmts.contains(myBitstream.getFormat(context).getShortDescription())) {
                try {
                    // only update item if bitstream not skipped
                    if (processBitstream(context, myItem, myBitstream, filterClass)) {
                        itemService.update(context, myItem); // Make sure new bitstream has a sequence
                        // number
                        filtered = true;
                    }
                } catch (Exception e) {
                    String handle = myItem.getHandle();
                    List<Bundle> bundles = myBitstream.getBundles();
                    long size = myBitstream.getSize();
                    String checksum = myBitstream.getChecksum() + " (" + myBitstream.getChecksumAlgorithm() + ")";
                    int assetstore = myBitstream.getStoreNumber();

                    // Printout helpful information to find the errored bitstream.
                    System.out.println("ERROR filtering, skipping bitstream:\n");
                    System.out.println("\tItem Handle: " + handle);
                    for (Bundle bundle : bundles) {
                        System.out.println("\tBundle Name: " + bundle.getName());
                    }
                    System.out.println("\tFile Size: " + size);
                    System.out.println("\tChecksum: " + checksum);
                    System.out.println("\tAsset Store: " + assetstore);
                    System.out.println(e);
                    e.printStackTrace();
                }
            } else if (filterClass instanceof SelfRegisterInputFormats) {
                // Filter implements self registration, so check to see if it should be applied
                // given the formats it claims to support
                SelfRegisterInputFormats srif = (SelfRegisterInputFormats) filterClass;
                boolean applyFilter = false;

                // Check MIME type
                String[] mimeTypes = srif.getInputMIMETypes();
                if (mimeTypes != null) {
                    for (String mimeType : mimeTypes) {
                        if (mimeType.equalsIgnoreCase(myBitstream.getFormat(context).getMIMEType())) {
                            applyFilter = true;
                        }
                    }
                }

                // Check description
                if (!applyFilter) {
                    String[] descriptions = srif.getInputDescriptions();
                    if (descriptions != null) {
                        for (String desc : descriptions) {
                            if (desc.equalsIgnoreCase(myBitstream.getFormat(context).getShortDescription())) {
                                applyFilter = true;
                            }
                        }
                    }
                }

                // Check extensions
                if (!applyFilter) {
                    String[] extensions = srif.getInputExtensions();
                    if (extensions != null) {
                        for (String ext : extensions) {
                            List<String> formatExtensions = myBitstream.getFormat(context).getExtensions();
                            if (formatExtensions != null && formatExtensions.contains(ext)) {
                                applyFilter = true;
                            }
                        }
                    }
                }

                // Filter claims to handle this type of file, so attempt to apply it
                if (applyFilter) {
                    try {
                        // only update item if bitstream not skipped
                        if (processBitstream(context, myItem, myBitstream, filterClass)) {
                            itemService.update(context, myItem); // Make sure new bitstream has a sequence
                            // number
                            filtered = true;
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR filtering, skipping bitstream #"
                                + myBitstream.getID() + " " + e);
                        e.printStackTrace();
                    }
                }
            }
        }
        return filtered;
    }
    
    @Override
    public boolean processBitstream(Context context, Item item, Bitstream source, FormatFilter formatFilter)
            throws Exception
    {
        //do pre-processing of this bitstream, and if it fails, skip this bitstream!
    	if(!formatFilter.preProcessBitstream(context, item, source, isVerbose))
        {
            return false;
        }
        	
    	boolean overWrite = isForce;
        
        // get bitstream filename, calculate destination filename
        String newName = formatFilter.getFilteredName(source.getName());

        Bitstream existingBitstream = null; // is there an existing rendition?
        Bundle targetBundle = null; // bundle we're modifying

        List<Bundle> bundles = itemService.getBundles(item, formatFilter.getBundleName());

        // check if destination bitstream exists
        if (bundles.size() > 0)
        {
            // only finds the last match (FIXME?)
            for (Bundle bundle : bundles) {
                List<Bitstream> bitstreams = bundle.getBitstreams();

                for (Bitstream bitstream : bitstreams) {
                    if (bitstream.getName().equals(newName)) {
                        targetBundle = bundle;
                        existingBitstream = bitstream;
                    }
                }
            }
        }

        // if exists and overwrite = false, exit
        if (!overWrite && (existingBitstream != null))
        {
            if (!isQuiet)
            {
                System.out.println("SKIPPED: bitstream " + source.getID()
                        + " (item: " + item.getHandle() + ") because '" + newName + "' already exists");
            }

            return false;
        }
        
        if(isVerbose) {
            System.out.println("PROCESSING: bitstream " + source.getID()
                + " (item: " + item.getHandle() + ")");
        }

        InputStream destStream;
        try {
            System.out.println("File: " + newName);
            destStream = formatFilter.getDestinationStream(item, bitstreamService.retrieve(context, source), isVerbose);
            if (destStream == null) {
                if (!isQuiet) {
                    System.out.println("SKIPPED: bitstream " + source.getID()
                            + " (item: " + item.getHandle() + ") because filtering was unsuccessful");
                }

                return false;
            }
        } catch (OutOfMemoryError oome) {
            System.out.println("!!! OutOfMemoryError !!!");
            return false;
        }

        // create new bundle if needed
        if (bundles.size() < 1)
        {
            targetBundle = bundleService.create(context, item, formatFilter.getBundleName());
        }
        else
        {
            // take the first match
            targetBundle = bundles.get(0);
        }

        Bitstream b = bitstreamService.create(context, targetBundle, destStream);

        // Now set the format and name of the bitstream
        b.setName(context, newName);
        b.setSource(context, "Written by FormatFilter " + formatFilter.getClass().getName() +
        			" on " + DCDate.getCurrent() + " (GMT)."); 
        b.setDescription(context, formatFilter.getDescription());

        // Find the proper format
        BitstreamFormat bf = bitstreamFormatService.findByShortDescription(context,
                formatFilter.getFormatString());
        bitstreamService.setFormat(context, b, bf);
        bitstreamService.update(context, b);
        
        //Set permissions on the derivative bitstream
        //- First remove any existing policies
        authorizeService.removeAllPolicies(context, b);

        //- Determine if this is a public-derivative format
        if(publicFiltersClasses.contains(formatFilter.getClass().getSimpleName())) {
            //- Set derivative bitstream to be publicly accessible
            Group anonymous = groupService.findByName(context, Group.ANONYMOUS);
            authorizeService.addPolicy(context, b, Constants.READ, anonymous);
        } else {
            //- Inherit policies from the source bitstream
            authorizeService.inheritPolicies(context, source, b);
        }

        // fixme - set date?
        // we are overwriting, so remove old bitstream
        if (existingBitstream != null)
        {
            bundleService.removeBitstream(context, targetBundle, existingBitstream);
        }

        if (!isQuiet)
        {
            System.out.println("FILTERED: bitstream " + source.getID()
                    + " (item: " + item.getHandle() + ") and created '" + newName + "'");
        }

        //do post-processing of the generated bitstream
        formatFilter.postProcessBitstream(context, item, b);
        
        return true;
    }
    
    @Override
    public Item getCurrentItem()
    {
        return currentItem;
    }
    
    @Override
    public boolean inSkipList(String identifier)
    {
        if(skipList!=null && skipList.contains(identifier))
        {
            if (!isQuiet)
            {
                System.out.println("SKIP-LIST: skipped bitstreams within identifier " + identifier);
            }
            return true;
        }    
        else
        {
            return false;
        }
    }

    @Override
    public void setVerbose(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    @Override
    public void setQuiet(boolean isQuiet) {
        this.isQuiet = isQuiet;
    }

    @Override
    public void setForce(boolean isForce) {
        this.isForce = isForce;
    }

    @Override
    public void setMax2Process(int max2Process) {
        this.max2Process = max2Process;
    }

    @Override
    public void setFilterClasses(List<FormatFilter> filterClasses) {
        this.filterClasses = filterClasses;
    }

    @Override
    public void setSkipList(List<String> skipList) {
        this.skipList = skipList;
    }

    @Override
    public void setFilterFormats(Map<String, List<String>> filterFormats) {
        this.filterFormats = filterFormats;
    }
}

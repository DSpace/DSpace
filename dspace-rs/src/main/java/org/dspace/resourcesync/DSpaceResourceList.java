/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.openarchives.resourcesync.ResourceList;
import org.openarchives.resourcesync.ResourceSyncDocument;
import org.openarchives.resourcesync.URL;
/**
 * @author Richard Jones
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
public class DSpaceResourceList extends DSpaceResourceDocument
{
	
	private final transient HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
	private final transient SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
	
    protected String metadataChangeFreq = null;
    protected String bitstreamChangeFreq = null;
    protected boolean dump = false;
    private static Logger log = Logger.getLogger(DSpaceResourceList.class);

    public DSpaceResourceList(Context context)
    {
        super(context);
        this.metadataChangeFreq = this.getMetadataChangeFreq();
        this.bitstreamChangeFreq = this.getBitstreamChangeFreq();
    }

    public DSpaceResourceList(Context context, boolean dump)
    {
        super(context);
        this.metadataChangeFreq = this.getMetadataChangeFreq();
        this.bitstreamChangeFreq = this.getBitstreamChangeFreq();
        this.dump = dump;
    }

    public DSpaceResourceList(Context context, List<String> exposeBundles, List<MetadataFormat> mdFormats,
                                String mdChangeFreq, String bitstreamChangeFreq, boolean dump)
    {
        super(context, exposeBundles, mdFormats);
        this.metadataChangeFreq = mdChangeFreq;
        this.bitstreamChangeFreq = bitstreamChangeFreq;
        this.dump = dump;
    }

    //used for resourcedump
    public void serialise(OutputStream out,String handle,UrlManager um)
            throws SQLException, IOException
    {
    	
        ResourceList rl = new ResourceList(um.capabilityList(), this.dump);
        DSpaceObject dSpaceObject = null;
        if (!handle.equals(siteService.findSite(context).getHandle())) {
        	dSpaceObject = handleService.resolveToObject(context, handle);
        }
        
        try {
			BrowseEngine be = new BrowseEngine(context);
	        BrowserScope bs = new BrowserScope(context);
	        bs.setBrowseIndex(BrowseIndex.getItemBrowseIndex());
	        
	        bs.setResultsPerPage(100);
	        if (dSpaceObject != null) {
	        	bs.setBrowseContainer(dSpaceObject);
	        }
	        boolean end = false;
	        int offset = 0;
	        while (!end) {
	        	BrowseInfo binfo = be.browse(bs);
	        	end = binfo.isLast();
	        	List<Item> items = binfo.getBrowseItemResults();
	        	for (Item it : items)
	        	{
	        		this.addResources(it, rl);
	        	}
	        	
	        	offset += binfo.getNextOffset();
	        	bs.setOffset(offset);
	        }

		} catch (BrowseException e) {
			log.error(e.getMessage(),e);				
		}

        rl.setLastModified(new Date());
        rl.serialise(out);
    }
    //used for changedump
    public void serialise(OutputStream out,UrlManager um,List<ResourceSyncEvent> rseList)
            throws SQLException, IOException
    {
        ResourceList rl = new ResourceList(null,um.capabilityList(), this.dump,this.dump);
		for (ResourceSyncEvent rse : rseList) {
			DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(rse.getResource_type());
			DSpaceObject dso = dsoService.find(context, rse.getResource_id());
			if (dso instanceof Item) {
				Item i = (Item) dso;

        		this.addResources(i, rl);
			}
		}
        rl.setLastModified(new Date());
        rl.serialise(out);
    }
    
    @Override
    protected URL addBitstream(Bitstream bitstream, Item item, List<Collection> collections, ResourceSyncDocument rl) throws SQLException
    {
        URL url = super.addBitstream(bitstream, item, collections, rl);
        url.setChangeFreq(this.bitstreamChangeFreq);
        return url;
    }

    @Override
    protected URL addMetadata(Item item, MetadataFormat format, List<Bitstream> describes, List<Collection> collections, ResourceSyncDocument rl)
    {
        URL url = super.addMetadata(item, format, describes, collections, rl);
        if (this.metadataChangeFreq != null)
        {
            url.setChangeFreq(this.metadataChangeFreq);
        }
        return url;
    }
    

}

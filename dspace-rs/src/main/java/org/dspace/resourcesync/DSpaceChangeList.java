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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.resourcesync.ResourceSyncAuditService.ChangeType;
import org.openarchives.resourcesync.ChangeList;
import org.openarchives.resourcesync.ResourceSync;
import org.openarchives.resourcesync.ResourceSyncDocument;
import org.openarchives.resourcesync.URL;
/**
 * @author Richard Jones
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 *
 */
public class DSpaceChangeList extends DSpaceResourceDocument {
	private boolean includeRestricted = false;
	private Date from;
	private Date to;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	public DSpaceChangeList(Context context, Date from, Date to, UrlManager um) {
		super(context);
		this.includeRestricted = ConfigurationManager.getBooleanProperty("resourcesync",
				"changelist.include-restricted");
		this.from = from;
		this.to = to;
		this.um = um;
	}

	public DSpaceChangeList(Context context, List<String> exposeBundles, List<MetadataFormat> mdFormats,
			boolean includeRestricted) {
		super(context, exposeBundles, mdFormats);
		this.includeRestricted = includeRestricted;
	}

	public void serialise(OutputStream out) throws SQLException, ParseException, IOException {
		// explicitly declare our arguments, for readability
		
		ChangeList cl = new ChangeList(from, to, this.um.capabilityList());
		cl.serialise(out);

	}

	public void serialiseForDump(OutputStream out, UrlManager um, List<ResourceSyncEvent> rseList)
			throws SQLException, IOException

	{

		ChangeList cl = new ChangeList(from, to, um.capabilityList());
		for (ResourceSyncEvent rse : rseList) {
			if (!rse.getChangetype().toLowerCase().equals(ChangeType.REMOVE.type()))
			{
				DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(rse.getResource_type());
				DSpaceObject dso = dsoService.find(context, rse.getResource_id());
				if (dso instanceof Item) {
					Item i = (Item) dso;
					cl.setChangeType(rse.getChangetype());
					this.addResources(i, cl);
				}
			}
			else
			{
				if (rse.getResource_type() == 2)
				{
					DSpaceResourceDocument dsrd = new DSpaceResourceDocument(context);
					List <MetadataFormat> mdfList = new ArrayList<MetadataFormat>();
					mdfList = dsrd.getMetadataFormats();
					for (MetadataFormat mdf : mdfList)
					{
						mdf.getPrefix();
						cl.setChangeType(rse.getChangetype());
						this.addResources(rse, cl,mdf.getPrefix());
					}
				}
				else
				{
					cl.setChangeType(rse.getChangetype());
					this.addResources(rse, cl,null);
				}
			}
		}

		cl.serialise(out);
	}

	 protected URL addResources(ResourceSyncEvent rse, ResourceSyncDocument rl,String format)
	            throws SQLException
	    {
		 	URL bs = new URL();
		 	String resourceSyncDir = ConfigurationManager.getProperty("resourcesync", "base-url");
		 	if (rse.getResource_type() == 0)
		 	{
		 		bs.setLoc(resourceSyncDir+"/bitstreams/" + rse.getResource_id());
		 	}
		 	else
		 	{
		 		bs.setLoc(resourceSyncDir+"/"+rse.getHandle()+"/"+format);

		 	}
		 	bs.setChange(ResourceSync.CHANGE_DELETED);
		 	rl.addEntry(bs);
		 	return bs;
	    }
	@Override
	protected URL addBitstream(Bitstream bitstream, Item item, List<Collection> collections, ResourceSyncDocument rl) throws SQLException {
		URL url = super.addBitstream(bitstream, item, collections, rl);
		// we can't ever know if an item is created in DSpace, as no such metadata
		// exists
		// all we can say is that it was updated
		String change = null;
		change = rl.getChangeType();
		if (item.isWithdrawn()) {
			// if the item is withdrawn, we say that the change that happened to it was
			// that it was deleted
			change = ResourceSync.CHANGE_DELETED;
		}
		url.setChange(change);
		return url;
	}

	@Override
	protected URL addMetadata(Item item, MetadataFormat format, List<Bitstream> describes, List<Collection> collections,
			ResourceSyncDocument rl) {
		URL url = super.addMetadata(item, format, describes, collections, rl);
		String change = null;
		change = rl.getChangeType();
		if (item.isWithdrawn()) {
			// if the item is withdrawn, we say that the change that happened to it was
			// that it was deleted
			change = ResourceSync.CHANGE_DELETED;
		}
		url.setChange(change);
		return url;
	}

}

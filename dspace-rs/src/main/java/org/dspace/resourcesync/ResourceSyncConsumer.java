/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package org.dspace.resourcesync;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.resourcesync.ResourceSyncAuditService.ChangeType;
import org.dspace.utils.DSpace;

/**
 * Class for audit changes relevant for resourcesync.
 * 
 * @version $Revision$
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
 
public class ResourceSyncConsumer implements Consumer {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ResourceSyncConsumer.class);

	private ResourceSyncAuditService resourceSyncAuditService;
	private final transient SiteService siteService = ContentServiceFactory.getInstance().getSiteService();

	public void initialize() throws Exception {
		resourceSyncAuditService = new DSpace().getServiceManager()
				.getServiceByName(ResourceSyncAuditService.class.getCanonicalName(), ResourceSyncAuditService.class);
	}

	/**
	 * Consume a content event -- just build the sets of objects to add (new) to the
	 * index, update, and delete.
	 * 
	 * @param ctx
	 *            DSpace context
	 * @param event
	 *            Content event
	 */
	public void consume(Context ctx, Event event) throws Exception {

		int st = event.getSubjectType();

		DSpaceObject subject = event.getSubject(ctx);
//		DSpaceObject object = event.getObject(ctx);

		// If event subject is a Bundle and event was Add or Remove,
		// transform the event to be a Modify on the owning Item.
		// It could be a new bitstream in the TEXT bundle which
		// would change the index.
//		int et = event.getEventType();

		switch (st) {
		case Constants.ITEM:
			consumeItemEvent(ctx, (Item) subject, event);
			break;

		case Constants.BUNDLE:
			consumeBundleEvent(ctx, (Bundle) subject, event);
			break;

		case Constants.BITSTREAM:
			consumeBistreamEvent(ctx, event);
			break;

		case Constants.COLLECTION:
			consumeCollectionEvent((Collection) subject, event);
			break;

		default:
			log.warn("ResourceSyncConsumer should not have been given this kind of Subject in an event, skipping: "
					+ event.toString());
			return;

		}
	}

	public void end(Context ctx) throws Exception {
		// No-op
	}

	public void finish(Context ctx) throws Exception {
		// No-op

	}

	private void consumeCollectionEvent(Collection collection, Event event) {
		int et = event.getEventType();
		UUID itemID = event.getObjectID();

		if (et != Event.ADD && et != Event.REMOVE) {
			return;
		}

		List<String> scopes = new ArrayList<String>();
		List<Community> community = null;
		switch (et) {
		case Event.ADD:
			try {
				community = collection.getCommunities();
			} catch (SQLException e) {
	        	log.error(e.getMessage(),e);
			}
			for (Community c : community) {
				scopes.add(c.getHandle());
			}
			scopes.add(collection.getHandle());
			addCreateEvent(Constants.ITEM, itemID, scopes,event.getDetail(),event.getIdentifiers());
			break;
		case Event.REMOVE:
			try {
				community = collection.getCommunities();
			} catch (SQLException e) {
	        	log.error(e.getMessage(),e);
			}
			for (Community c : community) {
				scopes.add(c.getHandle());
			}
			scopes.add(collection.getHandle());
			addRemoveEvent(Constants.ITEM, itemID, scopes,event.getDetail(),event.getIdentifiers());
			break;
		}
	}

	private void consumeBistreamEvent(Context context, Event event) throws SQLException {
		int et = event.getEventType();
		UUID bitstreamID = event.getSubjectID();

		if (et != Event.DELETE) {
			return;
		}
		List<String> scopes = new ArrayList<String>();
		scopes.add(siteService.findSite(context).getHandle());
		addRemoveEvent(Constants.BITSTREAM, bitstreamID, scopes,event.getDetail(),event.getIdentifiers());
	}

	private void consumeItemEvent(Context context, Item item, Event event) throws SQLException {
		int et = event.getEventType();
		UUID itemID = event.getSubjectID();
		Bundle bnd = (Bundle) event.getObject(context);

		if (et != Event.ADD && et != Event.REMOVE && et != Event.INSTALL && et != Event.MODIFY_METADATA
				&& et != Event.DELETE) {
			return;
		}

		List<String> scopes = new ArrayList<String>();
		switch (et) {
		case Event.INSTALL:
			scopes.add(siteService.findSite(context).getHandle());
			addCreateEvent(Constants.ITEM, itemID, scopes,event.getDetail(),event.getIdentifiers());
			break;
		case Event.MODIFY_METADATA:
			addUpdateEvent(Constants.ITEM, itemID, getScopes(context, item),event.getDetail(),event.getIdentifiers());
			break;
		case Event.ADD:

			if (isResourceSyncRelevant(bnd)) {
				for (Bitstream b : bnd.getBitstreams()) {
					addCreateEvent(Constants.BITSTREAM, b.getID(), getScopes(context, item),event.getDetail(),event.getIdentifiers());
				}
			}
			break;
		case Event.REMOVE:
			if (isResourceSyncRelevant(bnd)) {
				for (Bitstream b : bnd.getBitstreams()) {
					addRemoveEvent(Constants.BITSTREAM, b.getID(), getScopes(context, item),event.getDetail(),event.getIdentifiers());
				}
			}
			break;
		case Event.DELETE:
			scopes.add(siteService.findSite(context).getHandle());
			addRemoveEvent(Constants.ITEM, itemID, scopes,event.getDetail(),event.getIdentifiers());
			break;
		}
	}

	private boolean isResourceSyncRelevant(Bundle bnd) {
		if (bnd == null)
			return false;
		return ResourceSyncConfiguration.getBundlesToExpose().contains(bnd.getName());
	}

	private void consumeBundleEvent(Context context, Bundle bundle, Event event) throws SQLException {
		int et = event.getEventType();

		if (et != Event.ADD && et != Event.REMOVE) {
			return;
		}

		// if the bundle doesn't exist anymore we will deal with the REMOVE at the ITEM
		// level
		if (bundle == null) {
			return;
		}

		if (!isResourceSyncRelevant(bundle)) {
			return;
		}

		List<Item> items = bundle.getItems();

		for(Item item : items) {
			UUID bitID = event.getObjectID();
			List<String> scopes = getScopes(context, item);
			switch (et) {
			case Event.ADD:
				addCreateEvent(Constants.BITSTREAM, bitID, scopes,event.getDetail(),event.getIdentifiers());
				break;
			case Event.REMOVE:
				addRemoveEvent(Constants.BITSTREAM, bitID, scopes,event.getDetail(),event.getIdentifiers());
				break;
			}
		}
	}

	private void addCreateEvent(int resourcetype, UUID resourceID, List<String> scopes,String handle, List<String> identifiers) {
		addEvent(resourcetype, resourceID, ChangeType.CREATE, scopes,handle,identifiers);
	}

	private void addUpdateEvent(int resourcetype, UUID resourceID, List<String> scopes,String handle, List<String> identifiers) {
		addEvent(resourcetype, resourceID, ChangeType.UPDATE, scopes,handle,identifiers);

	}

	private void addRemoveEvent(int resourcetype, UUID resourceID, List<String> scopes,String handle, List<String> identifiers) {
		addEvent(resourcetype, resourceID, ChangeType.REMOVE, scopes,handle,identifiers);
	}

	private void addEvent(int resourcetype, UUID resourceID, ChangeType eventtype, List<String> scopes,String handle, List<String> identifiers) {
		resourceSyncAuditService.addEvent(resourceID, resourcetype, eventtype, new Date(), scopes,handle,identifiers);
	}

	private List<String> getScopes(Context context, Item item) throws SQLException {
		List<String> scopes = new ArrayList<String>();
		for (Collection c : item.getCollections()) {
			for (Community cc : c.getCommunities()) {
				scopes.add(cc.getHandle());
			}
			scopes.add(c.getHandle());
		}
		scopes.add(siteService.findSite(context).getHandle());
		return scopes;
	}

}

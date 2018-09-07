/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.authority.Choices;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Item object.
 * This class is responsible for all business logic calls for the Item object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ItemServiceImpl extends DSpaceObjectServiceImpl<Item> implements ItemService {

    /**
     * log4j category
     */
    private static final Logger log = Logger.getLogger(Item.class);

    @Autowired(required = true)
    protected ItemDAO itemDAO;

    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected MetadataSchemaService metadataSchemaService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected InstallItemService installItemService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected IdentifierService identifierService;
    @Autowired(required = true)
    protected VersioningService versioningService;
    @Autowired(required=true)
    protected HarvestedItemService harvestedItemService;
    @Autowired(required=true)
    protected ConfigurationService configurationService;
    
    @Autowired(required=true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required=true)
    protected WorkflowItemService workflowItemService;
    

    protected ItemServiceImpl()
    {
        super();
    }

    @Override
    public Thumbnail getThumbnail(Context context, Item item, boolean requireOriginal) throws SQLException {
        Bitstream thumbBitstream;
        List<Bundle> originalBundles = getBundles(item, "ORIGINAL");
        Bitstream primaryBitstream = null;
        if(CollectionUtils.isNotEmpty(originalBundles))
        {
            primaryBitstream = originalBundles.get(0).getPrimaryBitstream();
        }
        if (primaryBitstream != null) {
            if (primaryBitstream.getFormat(context).getMIMEType().equals("text/html")) {
                return null;
            }

            thumbBitstream = bitstreamService.getBitstreamByName(item, "THUMBNAIL", primaryBitstream.getName() + ".jpg");

        } else {
            if (requireOriginal) {
                primaryBitstream = bitstreamService.getFirstBitstream(item, "ORIGINAL");
            }

            thumbBitstream = bitstreamService.getFirstBitstream(item, "THUMBNAIL");
        }

        if (thumbBitstream != null) {
            return new Thumbnail(thumbBitstream, primaryBitstream);
        }

        return null;
    }

    @Override
    public Item find(Context context, UUID id) throws SQLException {
        Item item = itemDAO.findByID(context, Item.class, id);
        if (item == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogManager.getHeader(context, "find_item",
                        "not_found,item_id=" + id));
            }
            return null;
        }

        // not null, return item
        if (log.isDebugEnabled()) {
            log.debug(LogManager.getHeader(context, "find_item", "item_id="
                    + id));
        }

        return item;
    }

    @Override
    public Item create(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
        if (workspaceItem.getItem() != null) {
            throw new IllegalArgumentException("Attempting to create an item for a workspace item that already contains an item");
        }
        Item item = createItem(context);
        workspaceItem.setItem(item);


        log.info(LogManager.getHeader(context, "create_item", "item_id="
                + item.getID()));

        return item;
    }

    @Override
    public Item createTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException {
        if(collection == null || collection.getTemplateItem() != null)
        {
            throw new IllegalArgumentException("Collection is null or already contains template item.");
        }
        AuthorizeUtil.authorizeManageTemplateItem(context, collection);

        if (collection.getTemplateItem() == null) {
            Item template = createItem(context);
            collection.setTemplateItem(template);
            template.setTemplateItemOf(collection);

            log.info(LogManager.getHeader(context, "create_template_item",
                    "collection_id=" + collection.getID() + ",template_item_id="
                            + template.getID()));

            return template;
        }else{
            return collection.getTemplateItem();
        }
    }

    @Override
    public Iterator<Item> findAll(Context context) throws SQLException {
        return itemDAO.findAll(context, true);
    }

    @Override
    public Iterator<Item> findAllUnfiltered(Context context) throws SQLException {
        return itemDAO.findAll(context, true, true);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson) throws SQLException {
        return itemDAO.findBySubmitter(context, eperson);
    }

    @Override
    public Iterator<Item> findBySubmitterDateSorted(Context context, EPerson eperson, Integer limit) throws SQLException {

        MetadataField metadataField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "date", "accessioned");
        if(metadataField==null)
        {
            throw new IllegalArgumentException("Required metadata field '" + MetadataSchema.DC_SCHEMA + ".date.accessioned' doesn't exist!");
        }

        return itemDAO.findBySubmitter(context, eperson, metadataField, limit);
    }

    @Override
    public Iterator<Item> findByCollection(Context context, Collection collection) throws SQLException {
        return findByCollection(context, collection, null, null);
    }

    @Override
    public Iterator<Item> findByCollection(Context context, Collection collection, Integer limit, Integer offset) throws SQLException {
        return itemDAO.findArchivedByCollection(context, collection, limit, offset);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException {
        return itemDAO.findAllByCollection(context, collection);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection, Integer limit, Integer offset) throws SQLException {
        return itemDAO.findAllByCollection(context, collection, limit, offset);
    }

    @Override
    public Iterator<Item> findInArchiveOrWithdrawnDiscoverableModifiedSince(Context context, Date since)
            throws SQLException
    {
        return itemDAO.findAll(context, true, true, true, since);
    }
	
	@Override
    public Iterator<Item> findInArchiveOrWithdrawnNonDiscoverableModifiedSince(Context context, Date since)
            throws SQLException
    {
        return itemDAO.findAll(context, true, true, false, since);
    }
	
    @Override
    public void updateLastModified(Context context, Item item) throws SQLException, AuthorizeException {
        item.setLastModified(new Date());
        update(context, item);
        //Also fire a modified event since the item HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(), null, getIdentifiers(context, item)));
    }

    @Override
    public boolean isIn(Item item, Collection collection) throws SQLException {
        List<Collection> collections = item.getCollections();
        return collections != null && collections.contains(collection);
    }

    @Override
    public List<Community> getCommunities(Context context, Item item) throws SQLException {
        List<Community> result = new ArrayList<>();
        List<Collection> collections = item.getCollections();
        for (Collection collection : collections) {
            result.addAll(communityService.getAllParents(context, collection));
        }

        return result;
    }

    @Override
    public List<Bundle> getBundles(Item item, String name) throws SQLException {
        List<Bundle> matchingBundles = new ArrayList<>();
        // now only keep bundles with matching names
        List<Bundle> bunds = item.getBundles();
        for (Bundle bund : bunds) {
            if (name.equals(bund.getName())) {
                matchingBundles.add(bund);
            }
        }
        return matchingBundles;
    }

    @Override
    public void addBundle(Context context, Item item, Bundle bundle) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, item, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_bundle", "item_id="
                + item.getID() + ",bundle_id=" + bundle.getID()));

        // Check it's not already there
        if (item.getBundles().contains(bundle)) {
            // Bundle is already there; no change
            return;
        }

        // now add authorization policies from owning item
        // hmm, not very "multiple-inclusion" friendly
        authorizeService.inheritPolicies(context, item, bundle);

        // Add the bundle to in-memory list
        item.addBundle(bundle);
        bundle.addItem(item);

        context.addEvent(new Event(Event.ADD, Constants.ITEM, item.getID(),
                Constants.BUNDLE, bundle.getID(), bundle.getName(),
                getIdentifiers(context, item)));
    }

    @Override
    public void removeBundle(Context context, Item item, Bundle bundle) throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, item, Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_bundle", "item_id="
                + item.getID() + ",bundle_id=" + bundle.getID()));

        context.addEvent(new Event(Event.REMOVE, Constants.ITEM, item.getID(),
                Constants.BUNDLE, bundle.getID(), bundle.getName(), getIdentifiers(context, item)));

            bundleService.delete(context, bundle);
    }

    @Override
    public Bitstream createSingleBitstream(Context context, InputStream is, Item item, String name) throws AuthorizeException, IOException, SQLException {
        // Authorisation is checked by methods below
        // Create a bundle
        Bundle bnd = bundleService.create(context, item, name);
        Bitstream bitstream = bitstreamService.create(context, bnd, is);
        addBundle(context, item, bnd);

        // FIXME: Create permissions for new bundle + bitstream
        return bitstream;
    }

    @Override
    public Bitstream createSingleBitstream(Context context, InputStream is, Item item) throws AuthorizeException, IOException, SQLException {
        return createSingleBitstream(context, is, item, "ORIGINAL");
    }

    @Override
    public List<Bitstream> getNonInternalBitstreams(Context context, Item item) throws SQLException {
        List<Bitstream> bitstreamList = new ArrayList<>();

        // Go through the bundles and bitstreams picking out ones which aren't
        // of internal formats
        List<Bundle> bunds = item.getBundles();

        for (Bundle bund : bunds) {
            List<Bitstream> bitstreams = bund.getBitstreams();

            for (Bitstream bitstream : bitstreams) {
                if (!bitstream.getFormat(context).isInternal()) {
                    // Bitstream is not of an internal format
                    bitstreamList.add(bitstream);
                }
            }
        }

        return bitstreamList;
    }

    protected Item createItem(Context context) throws SQLException, AuthorizeException {
        Item item = itemDAO.create(context, new Item());
        // set discoverable to true (default)
        item.setDiscoverable(true);

        // Call update to give the item a last modified date. OK this isn't
        // amazingly efficient but creates don't happen that often.
        context.turnOffAuthorisationSystem();
        update(context, item);
        context.restoreAuthSystemState();

        context.addEvent(new Event(Event.CREATE, Constants.ITEM, item.getID(),
                null, getIdentifiers(context, item)));

        log.info(LogManager.getHeader(context, "create_item", "item_id=" + item.getID()));

        return item;
    }

    @Override
    public void removeDSpaceLicense(Context context, Item item) throws SQLException, AuthorizeException, IOException {
        // get all bundles with name "LICENSE" (these are the DSpace license
        // bundles)
        List<Bundle> bunds = getBundles(item, "LICENSE");

        for (Bundle bund : bunds) {
            // FIXME: probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            removeBundle(context, item, bund);
        }
    }


    @Override
    public void removeLicenses(Context context, Item item) throws SQLException, AuthorizeException, IOException {
        // Find the License format
        BitstreamFormat bf = bitstreamFormatService.findByShortDescription(context, "License");
        int licensetype = bf.getID();

        // search through bundles, looking for bitstream type license
        List<Bundle> bunds = item.getBundles();

        for (Bundle bund : bunds) {
            boolean removethisbundle = false;

            List<Bitstream> bits = bund.getBitstreams();

            for (Bitstream bit : bits) {
                BitstreamFormat bft = bit.getFormat(context);

                if (bft.getID() == licensetype) {
                    removethisbundle = true;
                }
            }


            // probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            if (removethisbundle) {
                removeBundle(context, item, bund);
            }
        }
    }

    @Override
    public void update(Context context, Item item) throws SQLException, AuthorizeException {
        // Check authorisation
        // only do write authorization if user is not an editor
        if (!canEdit(context, item))
        {
            authorizeService.authorizeAction(context, item, Constants.WRITE);
        }

        log.info(LogManager.getHeader(context, "update_item", "item_id="
                + item.getID()));

        super.update(context, item);

        // Set sequence IDs for bitstreams in item
        int sequence = 0;
        List<Bundle> bunds = item.getBundles();

        // find the highest current sequence number
        for (Bundle bund : bunds) {
            List<Bitstream> streams = bund.getBitstreams();

            for (Bitstream bitstream : streams) {
                if (bitstream.getSequenceID() > sequence) {
                    sequence = bitstream.getSequenceID();
                }
            }
        }

        // start sequencing bitstreams without sequence IDs
        sequence++;


        for (Bundle bund : bunds) {
            List<Bitstream> streams = bund.getBitstreams();

            for (Bitstream stream : streams) {
                if (stream.getSequenceID() < 0) {
                    stream.setSequenceID(sequence);
                    sequence++;
                    bitstreamService.update(context, stream);
//                    modified = true;
                }
            }
        }

        if (item.isMetadataModified() || item.isModified())
        {
            // Set the last modified date
            item.setLastModified(new Date());

            itemDAO.save(context, item);

            if(item.isMetadataModified()){
                context.addEvent(new Event(Event.MODIFY_METADATA, item.getType(), item.getID(), item.getDetails(), getIdentifiers(context, item)));
            }

            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(),
                    null, getIdentifiers(context, item)));
            item.clearModified();
            item.clearDetails();
        }
    }

    @Override
    public void withdraw(Context context, Item item) throws SQLException, AuthorizeException {
                // Check permission. User either has to have REMOVE on owning collection
        // or be COLLECTION_EDITOR of owning collection
        AuthorizeUtil.authorizeWithdrawItem(context, item);

        String timestamp = DCDate.getCurrent().toString();

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = context.getCurrentUser();

        // Build some provenance data while we're at it.
        StringBuilder prov = new StringBuilder();

        prov.append("Item withdrawn by ").append(e.getFullName()).append(" (")
                .append(e.getEmail()).append(") on ").append(timestamp).append("\n")
                .append("Item was in collections:\n");

        List<Collection> colls = item.getCollections();

        for (Collection coll : colls) {
            prov.append(coll.getName()).append(" (ID: ").append(coll.getID()).append(")\n");
        }

        // Set withdrawn flag. timestamp will be set; last_modified in update()
        item.setWithdrawn(true);

        // in_archive flag is now false
        item.setArchived(false);

        prov.append(installItemService.getBitstreamProvenanceMessage(context, item));

        addMetadata(context, item, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", prov.toString());

        // Update item in DB
        update(context, item);

        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(),
                "WITHDRAW", getIdentifiers(context, item)));

        // switch all READ authorization policies to WITHDRAWN_READ
        authorizeService.switchPoliciesAction(context, item, Constants.READ, Constants.WITHDRAWN_READ);
        for (Bundle bnd : item.getBundles()) {
        	authorizeService.switchPoliciesAction(context, bnd, Constants.READ, Constants.WITHDRAWN_READ);
        	for (Bitstream bs : bnd.getBitstreams()) {
        		authorizeService.switchPoliciesAction(context, bs, Constants.READ, Constants.WITHDRAWN_READ);
        	}
        }

        // Write log
        log.info(LogManager.getHeader(context, "withdraw_item", "user="
                + e.getEmail() + ",item_id=" + item.getID()));
    }

    @Override
    public void reinstate(Context context, Item item) throws SQLException, AuthorizeException {
                // check authorization
        AuthorizeUtil.authorizeReinstateItem(context, item);

        String timestamp = DCDate.getCurrent().toString();

        // Check permission. User must have ADD on all collections.
        // Build some provenance data while we're at it.
        List<Collection> colls = item.getCollections();

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = context.getCurrentUser();
        StringBuilder prov = new StringBuilder();
        prov.append("Item reinstated by ").append(e.getFullName()).append(" (")
                .append(e.getEmail()).append(") on ").append(timestamp).append("\n")
                .append("Item was in collections:\n");

        for (Collection coll : colls) {
            prov.append(coll.getName()).append(" (ID: ").append(coll.getID()).append(")\n");
        }

        // Clear withdrawn flag
        item.setWithdrawn(false);

        // in_archive flag is now true
        item.setArchived(true);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        prov.append(installItemService.getBitstreamProvenanceMessage(context, item));

        addMetadata(context, item, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", prov.toString());

        // Update item in DB
        update(context, item);

        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(),
                "REINSTATE", getIdentifiers(context, item)));

		// restore all WITHDRAWN_READ authorization policies back to READ
		for (Bundle bnd : item.getBundles()) {
			authorizeService.switchPoliciesAction(context, bnd, Constants.WITHDRAWN_READ, Constants.READ);
			for (Bitstream bs : bnd.getBitstreams()) {
				authorizeService.switchPoliciesAction(context, bs, Constants.WITHDRAWN_READ, Constants.READ);
			}
		}

        // check if the item was withdrawn before the fix DS-3097
        if (authorizeService.getPoliciesActionFilter(context, item, Constants.WITHDRAWN_READ).size() != 0) {
        	authorizeService.switchPoliciesAction(context, item, Constants.WITHDRAWN_READ, Constants.READ);
        }
        else {
	        // authorization policies
	        if (colls.size() > 0)
	        {
	            // remove the item's policies and replace them with
	            // the defaults from the collection
	        	adjustItemPolicies(context, item, item.getOwningCollection());
	        }
        }
        
        // Write log
        log.info(LogManager.getHeader(context, "reinstate_item", "user="
                + e.getEmail() + ",item_id=" + item.getID()));
    }

    @Override
    public void delete(Context context, Item item) throws SQLException, AuthorizeException, IOException {
        authorizeService.authorizeAction(context, item, Constants.DELETE);
        rawDelete(context,  item);
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.ITEM;
    }

    protected void rawDelete(Context context, Item item) throws AuthorizeException, SQLException, IOException {
        authorizeService.authorizeAction(context, item, Constants.REMOVE);

        context.addEvent(new Event(Event.DELETE, Constants.ITEM, item.getID(),
                item.getHandle(), getIdentifiers(context, item)));

        log.info(LogManager.getHeader(context, "delete_item", "item_id="
                + item.getID()));

        // Remove bundles
        removeAllBundles(context, item);

        // Remove any Handle
        handleService.unbindHandle(context, item);
        
        // remove version attached to the item
        removeVersion(context, item);

        // Also delete the item if it appears in a harvested collection.
        HarvestedItem hi = harvestedItemService.find(context, item);

        if(hi!=null)
        {
            harvestedItemService.delete(context, hi);
        }

        //Only clear collections after we have removed everything else from the item
        item.clearCollections();
        item.setOwningCollection(null);

        // Finally remove item row
        itemDAO.delete(context, item);
    }

    @Override
    public void removeAllBundles(Context context, Item item) throws AuthorizeException, SQLException, IOException {
        Iterator<Bundle> bundles = item.getBundles().iterator();
        while(bundles.hasNext())
        {
            Bundle bundle = bundles.next();
            bundles.remove();
            deleteBundle(context, item, bundle);
        }
    }

    protected void deleteBundle(Context context, Item item, Bundle b) throws AuthorizeException, SQLException, IOException {
                         // Check authorisation
       authorizeService.authorizeAction(context, item, Constants.REMOVE);

       bundleService.delete(context, b);

       log.info(LogManager.getHeader(context, "remove_bundle", "item_id="
               + item.getID() + ",bundle_id=" + b.getID()));
       context.addEvent(new Event(Event.REMOVE, Constants.ITEM, item.getID(), Constants.BUNDLE, b.getID(), b.getName()));
   }

    protected void removeVersion(Context context, Item item) throws AuthorizeException, SQLException
    {
        if(versioningService.getVersion(context, item)!=null)
        {
            versioningService.removeVersion(context, item);
        }else{
            try {
                identifierService.delete(context, item);
            } catch (IdentifierException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isOwningCollection(Item item, Collection collection) {
        Collection owningCollection = item.getOwningCollection();

        return owningCollection != null && collection.getID().equals(owningCollection.getID());
    }

    @Override
    public void replaceAllItemPolicies(Context context, Item item, List<ResourcePolicy> newpolicies) throws SQLException, AuthorizeException {
        // remove all our policies, add new ones
        authorizeService.removeAllPolicies(context, item);
        authorizeService.addPolicies(context, newpolicies, item);
    }

    @Override
    public void replaceAllBitstreamPolicies(Context context, Item item, List<ResourcePolicy> newpolicies) throws SQLException, AuthorizeException {
        // remove all policies from bundles, add new ones
        List<Bundle> bunds = item.getBundles();

        for (Bundle mybundle : bunds) {
            bundleService.replaceAllBitstreamPolicies(context, mybundle, newpolicies);
        }
    }

    @Override
    public void removeGroupPolicies(Context context, Item item, Group group) throws SQLException, AuthorizeException {
        // remove Group's policies from Item
        authorizeService.removeGroupPolicies(context, item, group);

        // remove all policies from bundles
        List<Bundle> bunds = item.getBundles();

        for (Bundle mybundle : bunds) {
            List<Bitstream> bs = mybundle.getBitstreams();

            for (Bitstream bitstream : bs) {
                // remove bitstream policies
                authorizeService.removeGroupPolicies(context, bitstream, group);
            }

            // change bundle policies
            authorizeService.removeGroupPolicies(context, mybundle, group);
        }
    }

    @Override
    public void inheritCollectionDefaultPolicies(Context context, Item item, Collection collection) throws SQLException, AuthorizeException {
        adjustItemPolicies(context, item, collection);
        adjustBundleBitstreamPolicies(context, item, collection);

        log.debug(LogManager.getHeader(context, "item_inheritCollectionDefaultPolicies",
                                                   "item_id=" + item.getID()));
    }

    @Override
    public void adjustBundleBitstreamPolicies(Context context, Item item, Collection collection) throws SQLException, AuthorizeException {
        List<ResourcePolicy> defaultCollectionPolicies = authorizeService.getPoliciesActionFilter(context, collection, Constants.DEFAULT_BITSTREAM_READ);

        if (defaultCollectionPolicies.size() < 1){
            throw new SQLException("Collection " + collection.getID()
                    + " (" + collection.getHandle() + ")"
                    + " has no default bitstream READ policies");
        }

        // remove all policies from bundles, add new ones
        // Remove bundles
        List<Bundle> bunds = item.getBundles();
        for (Bundle mybundle : bunds) {

            // if come from InstallItem: remove all submission/workflow policies
            authorizeService.removeAllPoliciesByDSOAndType(context, mybundle, ResourcePolicy.TYPE_SUBMISSION);
            authorizeService.removeAllPoliciesByDSOAndType(context, mybundle, ResourcePolicy.TYPE_WORKFLOW);
            addDefaultPoliciesNotInPlace(context, mybundle, defaultCollectionPolicies);

            for(Bitstream bitstream : mybundle.getBitstreams())
            {
                // if come from InstallItem: remove all submission/workflow policies
                authorizeService.removeAllPoliciesByDSOAndType(context, bitstream, ResourcePolicy.TYPE_SUBMISSION);
                authorizeService.removeAllPoliciesByDSOAndType(context, bitstream, ResourcePolicy.TYPE_WORKFLOW);
                addDefaultPoliciesNotInPlace(context, bitstream, defaultCollectionPolicies);
            }
        }
    }

    @Override
    public void adjustItemPolicies(Context context, Item item, Collection collection) throws SQLException, AuthorizeException {
                // read collection's default READ policies
        List<ResourcePolicy> defaultCollectionPolicies = authorizeService.getPoliciesActionFilter(context, collection, Constants.DEFAULT_ITEM_READ);

        // MUST have default policies
        if (defaultCollectionPolicies.size() < 1)
        {
            throw new SQLException("Collection " + collection.getID()
                    + " (" + collection.getHandle() + ")"
                    + " has no default item READ policies");
        }

        try {
            //ignore the authorizations for now.
            context.turnOffAuthorisationSystem();

            // if come from InstallItem: remove all submission/workflow policies
            authorizeService.removeAllPoliciesByDSOAndType(context, item, ResourcePolicy.TYPE_SUBMISSION);
            authorizeService.removeAllPoliciesByDSOAndType(context, item, ResourcePolicy.TYPE_WORKFLOW);

            // add default policies only if not already in place
            addDefaultPoliciesNotInPlace(context, item, defaultCollectionPolicies);
        } 
        finally 
        {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public void move(Context context, Item item, Collection from, Collection to) throws SQLException, AuthorizeException, IOException {
        // Use the normal move method, and default to not inherit permissions
        this.move(context, item, from, to, false);
    }

    @Override
    public void move(Context context, Item item, Collection from, Collection to, boolean inheritDefaultPolicies) throws SQLException, AuthorizeException, IOException {
                // Check authorisation on the item before that the move occur
        // otherwise we will need edit permission on the "target collection" to archive our goal
        // only do write authorization if user is not an editor
        if (!canEdit(context, item))
        {
            authorizeService.authorizeAction(context, item, Constants.WRITE);
        }

        // Move the Item from one Collection to the other
        collectionService.addItem(context, to, item);
        collectionService.removeItem(context, from, item);

        // If we are moving from the owning collection, update that too
        if (isOwningCollection(item, from))
        {
            // Update the owning collection
            log.info(LogManager.getHeader(context, "move_item",
                                          "item_id=" + item.getID() + ", from " +
                                          "collection_id=" + from.getID() + " to " +
                                          "collection_id=" + to.getID()));
            item.setOwningCollection(to);

            // If applicable, update the item policies
            if (inheritDefaultPolicies)
            {
                log.info(LogManager.getHeader(context, "move_item",
                         "Updating item with inherited policies"));
                inheritCollectionDefaultPolicies(context, item, to);
            }

            // Update the item
            context.turnOffAuthorisationSystem();
            update(context, item);
            context.restoreAuthSystemState();
        }
        else
        {
            // Although we haven't actually updated anything within the item
            // we'll tell the event system that it has, so that any consumers that
            // care about the structure of the repository can take account of the move

            // Note that updating the owning collection above will have the same effect,
            // so we only do this here if the owning collection hasn't changed.

            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(),
                    null, getIdentifiers(context, item)));
        }
    }

    @Override
    public boolean hasUploadedFiles(Item item) throws SQLException {
        List<Bundle> bundles = getBundles(item, "ORIGINAL");
        for (Bundle bundle : bundles) {
            if (CollectionUtils.isNotEmpty(bundle.getBitstreams())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Collection> getCollectionsNotLinked(Context context, Item item) throws SQLException {
        List<Collection> allCollections = collectionService.findAll(context);
        List<Collection> linkedCollections = item.getCollections();
        List<Collection> notLinkedCollections = new ArrayList<>(allCollections.size() - linkedCollections.size());

        if ((allCollections.size() - linkedCollections.size()) == 0)
        {
            return notLinkedCollections;
        }
        for (Collection collection : allCollections)
        {
                 boolean alreadyLinked = false;
                 for (Collection linkedCommunity : linkedCollections)
                 {
                     if (collection.getID().equals(linkedCommunity.getID()))
                     {
                             alreadyLinked = true;
                             break;
                     }
                 }

                 if (!alreadyLinked)
                 {
                     notLinkedCollections.add(collection);
                 }
        }

        return notLinkedCollections;
    }

    @Override
    public boolean canEdit(Context context, Item item) throws SQLException {
        // can this person write to the item?
        if (authorizeService.authorizeActionBoolean(context, item,
                Constants.WRITE))
        {
            return true;
        }

        // is this collection not yet created, and an item template is created
        if (item.getOwningCollection() == null)
        {
        	if (!isInProgressSubmission(context, item)) {
        		return true;
        	}
        	else {
        		return false;
        	}
        }

        return collectionService.canEditBoolean(context, item.getOwningCollection(), false);
    }

    /**
     * Check if the item is an inprogress submission
     * @param context
     * @param item
     * @return <code>true</code> if the item is an inprogress submission, i.e. a WorkspaceItem or WorkflowItem
     * @throws SQLException
     */
    public boolean isInProgressSubmission(Context context, Item item) throws SQLException {
		return workspaceItemService.findByItem(context, item) != null
				|| workflowItemService.findByItem(context, item) != null;
    }
    
    /*
    With every finished submission a bunch of resource policy entries with have null value for the dspace_object column are generated in the database.
prevent the generation of resource policy entry values with null dspace_object as value

    */

    /**
     * Add the default policies, which have not been already added to the given DSpace object
     * 
     * @param context
     * @param dso
     * @param defaultCollectionPolicies
     * @throws SQLException
     * @throws AuthorizeException 
     */
    protected void addDefaultPoliciesNotInPlace(Context context, DSpaceObject dso, List<ResourcePolicy> defaultCollectionPolicies) throws SQLException, AuthorizeException
    {
            for (ResourcePolicy defaultPolicy : defaultCollectionPolicies)
            {
                if (!authorizeService.isAnIdenticalPolicyAlreadyInPlace(context, dso, defaultPolicy.getGroup(), Constants.READ, defaultPolicy.getID()))
                {
                    ResourcePolicy newPolicy = resourcePolicyService.clone(context, defaultPolicy);
                    newPolicy.setdSpaceObject(dso);
                    newPolicy.setAction(Constants.READ);
                    newPolicy.setRpType(ResourcePolicy.TYPE_INHERITED);
                    resourcePolicyService.update(context, newPolicy);
                }
            }
    }

    /**
     * Returns an iterator of Items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     *
     */
    @Override
    public Iterator<Item> findByMetadataField(Context context,
               String schema, String element, String qualifier, String value)
          throws SQLException, AuthorizeException, IOException
    {
        MetadataSchema mds = metadataSchemaService.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = metadataFieldService.findByElement(context, mds, element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException(
                    "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }

        if (Item.ANY.equals(value))
        {
            return itemDAO.findByMetadataField(context, mdf, null, true);
        }
        else
        {
            return itemDAO.findByMetadataField(context, mdf, value, true);
        }
    }

    @Override
    public Iterator<Item> findByMetadataQuery(Context context, List<List<MetadataField>> listFieldList, List<String> query_op, List<String> query_val, List<UUID> collectionUuids, String regexClause, int offset, int limit)
          throws SQLException, AuthorizeException, IOException
    {
        return itemDAO.findByMetadataQuery(context, listFieldList, query_op, query_val, collectionUuids, regexClause, offset, limit);
    }

    @Override
    public DSpaceObject getAdminObject(Context context, Item item, int action) throws SQLException {
        DSpaceObject adminObject = null;
        //Items are always owned by collections
        Collection collection = (Collection) getParentObject(context, item);
        Community community = null;
        if (collection != null)
        {
            if(CollectionUtils.isNotEmpty(collection.getCommunities()))
            {
                community = collection.getCommunities().get(0);
            }
        }

        switch (action)
        {
            case Constants.ADD:
                // ADD a cc license is less general than add a bitstream but we can't/won't
                // add complex logic here to know if the ADD action on the item is required by a cc or
                // a generic bitstream so simply we ignore it.. UI need to enforce the requirements.
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamCreation())
                {
                    adminObject = item;
                }
                else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamCreation())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformBitstreamCreation())
                {
                    adminObject = community;
                }
                break;
            case Constants.REMOVE:
                // see comments on ADD action, same things...
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamDeletion())
                {
                    adminObject = item;
                }
                else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamDeletion())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformBitstreamDeletion())
                {
                    adminObject = community;
                }
                break;
            case Constants.DELETE:
                if (item.getOwningCollection() != null)
                {
                    if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminPerformItemDeletion())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                break;
            case Constants.WRITE:
                // if it is a template item we need to check the
                // collection/community admin configuration
                if (item.getOwningCollection() == null)
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    adminObject = item;
                }
                break;
            default:
                adminObject = item;
                break;
            }
        return adminObject;
    }

    @Override
    public DSpaceObject getParentObject(Context context, Item item) throws SQLException {
        Collection ownCollection = item.getOwningCollection();
        if (ownCollection != null)
        {
            return ownCollection;
        }
        else
        {
            // is a template item?
            return item.getTemplateItemOf();
        }
    }

    @Override
    public Iterator<Item> findByAuthorityValue(Context context, String schema, String element, String qualifier, String value) throws SQLException, AuthorizeException {
        MetadataSchema mds = metadataSchemaService.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = metadataFieldService.findByElement(context, mds, element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException("No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }

        return itemDAO.findByAuthorityValue(context, mdf, value, true);
    }

    @Override
    public Iterator<Item> findByMetadataFieldAuthority(Context context, String mdString, String authority) throws SQLException, AuthorizeException {
        String[] elements = getElementsFilled(mdString);
        String schema = elements[0], element = elements[1], qualifier = elements[2];
        MetadataSchema mds = metadataSchemaService.find(context, schema);
        if (mds == null) {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = metadataFieldService.findByElement(context, mds, element, qualifier);
        if (mdf == null) {
            throw new IllegalArgumentException(
                    "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }
        return findByAuthorityValue(context, mds.getName(), mdf.getElement(), mdf.getQualifier(), authority);
    }

    @Override
    public boolean isItemListedForUser(Context context, Item item) {
        try {
            if (authorizeService.isAdmin(context)) {
                return true;
            }
            if (authorizeService.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                if(item.isDiscoverable()) {
                    return true;
                }
            }
            log.debug("item(" + item.getID() + ") " + item.getName() + " is unlisted.");
            return false;
        } catch (SQLException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public int countItems(Context context, Collection collection) throws SQLException {
        return itemDAO.countItems(context, collection, true, false);
    }

    @Override
    public int countAllItems(Context context, Collection collection) throws SQLException {
        return itemDAO.countItems(context, collection, true, false) + itemDAO.countItems(context, collection, false, true);
    }
    
    @Override
    public int countItems(Context context, Community community) throws SQLException {
        // First we need a list of all collections under this community in the hierarchy
        List<Collection> collections = communityService.getAllCollections(context, community);
        
        // Now, lets count unique items across that list of collections
        return itemDAO.countItems(context, collections, true, false);
    }
    
    @Override
    public int countAllItems(Context context, Community community) throws SQLException {
        // First we need a list of all collections under this community in the hierarchy
        List<Collection> collections = communityService.getAllCollections(context, community);
        
        // Now, lets count unique items across that list of collections
        return itemDAO.countItems(context, collections, true, false) + itemDAO.countItems(context, collections, false, true);
    }

    @Override
    protected void getAuthoritiesAndConfidences(String fieldKey, Collection collection, List<String> values, List<String> authorities, List<Integer> confidences, int i) {
        Choices c = choiceAuthorityService.getBestMatch(fieldKey, values.get(i), collection, null);
        authorities.add(c.values.length > 0 ? c.values[0].authority : null);
        confidences.add(c.confidence);
    }

    @Override
    public Item findByIdOrLegacyId(Context context, String id) throws SQLException {
        if(StringUtils.isNumeric(id))
        {
            return findByLegacyId(context, Integer.parseInt(id));
        }
        else
        {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public Item findByLegacyId(Context context, int id) throws SQLException {
        return itemDAO.findByLegacyId(context, id, Item.class);
    }

    @Override
    public Iterator<Item> findByLastModifiedSince(Context context, Date last)
            throws SQLException
    {
        return itemDAO.findByLastModifiedSince(context, last);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return itemDAO.countRows(context);
    }

    @Override
    public int countNotArchivedItems(Context context) throws SQLException {
        // return count of items not in archive and also not withdrawn
        return itemDAO.countItems(context, false, false);
    }

    @Override
    public int countWithdrawnItems(Context context) throws SQLException {
       // return count of items that are not in archive and withdrawn
       return itemDAO.countItems(context, false, true);
    }

    @Override
    public boolean canCreateNewVersion(Context context, Item item) throws SQLException{
        if (authorizeService.isAdmin(context, item)) 
        {
            return true;
        }

        if (context.getCurrentUser() != null
                && context.getCurrentUser().equals(item.getSubmitter())) 
        {
            return configurationService.getPropertyAsType(
                    "versioning.submitterCanCreateNewVersion", false);
        }

        return false;
    }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.dao.BundleBitstreamDAO;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation for the Bundle object.
 * This class is responsible for all business logic calls for the Bundle object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BundleServiceImpl extends DSpaceObjectServiceImpl<Bundle> implements BundleService {

    /** log4j logger */
    private static Logger log = Logger.getLogger(Bundle.class);

    @Autowired(required = true)
    protected BundleDAO bundleDAO;
    @Autowired(required = true)
    protected BundleBitstreamDAO bundleBitstreamDAO;


    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    @Override
    public Bundle find(Context context, UUID id) throws SQLException
    {
        // First check the cache
        Bundle bundle = bundleDAO.findByID(context, Bundle.class, id);
        if (bundle == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_bundle",
                        "not_found,bundle_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_bundle",
                        "bundle_id=" + id));
            }

            return bundle;
        }
    }

    @Override
    public Bundle create(Context context, Item item, String name) throws SQLException, AuthorizeException {
        if (StringUtils.isBlank(name))
        {
            throw new SQLException("Bundle must be created with non-null name");
        }
        authorizeService.authorizeAction(context, item, Constants.ADD);


        // Create a table row
        Bundle bundle = bundleDAO.create(context, new Bundle());
        bundle.setName(context, name);
        itemService.addBundle(context, item, bundle);
        if(!bundle.getItems().contains(item))
        {
            bundle.addItem(item);
        }


        log.info(LogManager.getHeader(context, "create_bundle", "bundle_id="
                + bundle.getID()));

        // if we ever use the identifier service for bundles, we should
        // create the bundle before we create the Event and should add all
        // identifiers to it.
        context.addEvent(new Event(Event.CREATE, Constants.BUNDLE, bundle.getID(), null));

        return bundle;
    }

    @Override
    public Bitstream getBitstreamByName(Bundle bundle, String name) {
        Bitstream target = null;

        for (BundleBitstream bundleBitstream : bundle.getBitstreams()) {
            Bitstream bitstream = bundleBitstream.getBitstream();
            if (name.equals(bitstream.getName())) {
                target = bitstream;
                break;
            }
        }

        return target;
    }

    @Override
    public void addBitstream(Context context, Bundle bundle, Bitstream bitstream) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_bitstream", "bundle_id="
                + bundle.getID() + ",bitstream_id=" + bitstream.getID()));

        // First check that the bitstream isn't already in the list
        List<BundleBitstream> bundleBitstreams = bundle.getBitstreams();
        int topOrder = 0;
        // First check that the bitstream isn't already in the list
        for (BundleBitstream bundleBitstream : bundleBitstreams) {
            if (bitstream.getID().equals(bundleBitstream.getBitstream().getID())) {
                // Bitstream is already there; no change
                return;
            }
            //The last file we encounter will have the highest order
            topOrder = bundleBitstream.getBitstreamOrder();
        }

        // Add the bitstream object
        BundleBitstream bundleBitstream = new BundleBitstream();
        bundleBitstream.setBitstream(bitstream);
        bundleBitstream.setBundle(bundle);

        bundleBitstream = bundleBitstreamDAO.create(context, bundleBitstream);
        bundleBitstream.setBitstreamOrder(topOrder++);
        bundleBitstreamDAO.save(context, bundleBitstream);
        bundle.addBitstream(bundleBitstream);
        bitstream.getBundles().add(bundleBitstream);


        context.addEvent(new Event(Event.ADD, Constants.BUNDLE, bundle.getID(),
                Constants.BITSTREAM, bitstream.getID(), String.valueOf(bitstream.getSequenceID()),
                getIdentifiers(context, bundle)));

        // copy authorization policies from bundle to bitstream
        // FIXME: multiple inclusion is affected by this...
        authorizeService.inheritPolicies(context, bundle, bitstream);
        bitstreamService.update(context, bitstream);
    }

    @Override
    public void removeBitstream(Context context, Bundle bundle, Bitstream bitstream) throws AuthorizeException, SQLException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_bitstream",
                "bundle_id=" + bundle.getID() + ",bitstream_id=" + bitstream.getID()));

        // Remove from internal list of bitstreams
        Iterator<BundleBitstream> li = bundle.getBitstreams().iterator();
        while (li.hasNext())
        {
            BundleBitstream bundleBitstream = li.next();

            if (bitstream.getID().equals(bundleBitstream.getBitstream().getID()))
            {
                // We've found the bitstream to remove
                li.remove();
                bitstream.getBundles().remove(bundleBitstream);
                bundle.getBitstreams().remove(bundleBitstream);
                bundleBitstreamDAO.delete(context, bundleBitstream);
            }
        }

        context.addEvent(new Event(Event.REMOVE, Constants.BUNDLE, bundle.getID(),
                Constants.BITSTREAM, bitstream.getID(), String.valueOf(bitstream.getSequenceID()),
                getIdentifiers(context, bundle)));

        //Ensure that the last modified from the item is triggered !
        Item owningItem = (Item) getParentObject(context, bundle);
        if(owningItem != null)
        {
            itemService.updateLastModified(context, owningItem);
            itemService.update(context, owningItem);
        }

        // In the event that the bitstream to remove is actually
        // the primary bitstream, be sure to unset the primary
        // bitstream.
        if (bitstream.equals(bundle.getPrimaryBitstream()))
        {
            bundle.unsetPrimaryBitstreamID();
        }

        bitstreamService.delete(context, bitstream);
    }

    @Override
    public void inheritCollectionDefaultPolicies(Context context, Bundle bundle, Collection collection) throws SQLException, AuthorizeException {
        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, collection,
                Constants.DEFAULT_BITSTREAM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        Iterator<ResourcePolicy> i = policies.iterator();

        if (!i.hasNext())
        {
            throw new java.sql.SQLException("Collection " + collection.getID()
                    + " has no default bitstream READ policies");
        }

        List<ResourcePolicy> newPolicies = new ArrayList<ResourcePolicy>();
        while (i.hasNext())
        {
            ResourcePolicy rp = resourcePolicyService.clone(context, i.next());
            rp.setAction(Constants.READ);
            newPolicies.add(rp);
        }

        replaceAllBitstreamPolicies(context, bundle, newPolicies);
    }

    @Override
    public void replaceAllBitstreamPolicies(Context context, Bundle bundle, List<ResourcePolicy> newpolicies) throws SQLException, AuthorizeException {
        List<BundleBitstream> bundleBitstreams = bundle.getBitstreams();
        if (CollectionUtils.isNotEmpty(bundleBitstreams))
        {
            for (BundleBitstream bundleBitstream : bundleBitstreams)
            {
                Bitstream bs = bundleBitstream.getBitstream();
                // change bitstream policies
                authorizeService.removeAllPolicies(context, bs);
                authorizeService.addPolicies(context, newpolicies, bs);
            }
        }
        // change bundle policies
        authorizeService.removeAllPolicies(context, bundle);
        authorizeService.addPolicies(context, newpolicies, bundle);
    }

    @Override
    public List<ResourcePolicy> getBitstreamPolicies(Context context, Bundle bundle) throws SQLException {
        List<ResourcePolicy> list = new ArrayList<ResourcePolicy>();
        List<BundleBitstream> bitstreams = bundle.getBitstreams();
        if (CollectionUtils.isNotEmpty(bitstreams))
        {
            for (BundleBitstream bs : bitstreams)
            {
                list.addAll(authorizeService.getPolicies(context, bs.getBitstream()));
            }
        }
        return list;
    }

    @Override
    public List<ResourcePolicy> getBundlePolicies(Context context, Bundle bundle) throws SQLException {
        return authorizeService.getPolicies(context, bundle);
    }

    @Override
    public void setOrder(Context context, Bundle bundle, UUID[] bitstreamIds) throws AuthorizeException, SQLException {
        authorizeService.authorizeAction(context, bundle, Constants.WRITE);

        for (int i = 0; i < bitstreamIds.length; i++) {
            UUID bitstreamId = bitstreamIds[i];
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if(bitstream == null){
                //This should never occur but just in case
                log.warn(LogManager.getHeader(context, "Invalid bitstream id while changing bitstream order", "Bundle: " + bundle.getID() + ", bitstream id: " + bitstreamId));
                continue;
            }

            List<BundleBitstream> bitstreamBundles = bitstream.getBundles();
            for (BundleBitstream bundleBitstream : bitstreamBundles) {
                bundleBitstream.setBitstreamOrder(i);
                bundleBitstreamDAO.save(context, bundleBitstream);

            }
            bitstreamService.update(context, bitstream);
        }

        //The order of the bitstreams has changed, ensure that we update the last modified of our item
        Item owningItem = (Item) getParentObject(context, bundle);
        if(owningItem != null)
        {
            itemService.updateLastModified(context, owningItem);
            itemService.update(context, owningItem);

        }
    }

    @Override
    public DSpaceObject getAdminObject(Context context, Bundle bundle, int action) throws SQLException {
        DSpaceObject adminObject = null;
        Item item = (Item) getParentObject(context, bundle);
        Collection collection = null;
        Community community = null;
        if (item != null)
        {
            collection = item.getOwningCollection();
            if (collection != null)
            {
                community = collection.getCommunities().iterator().next();
            }
        }
        switch (action)
        {
        case Constants.REMOVE:
            if (AuthorizeConfiguration.canItemAdminPerformBitstreamDeletion())
            {
                adminObject = item;
            }
            else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamDeletion())
            {
                adminObject = collection;
            }
            else if (AuthorizeConfiguration
                    .canCommunityAdminPerformBitstreamDeletion())
            {
                adminObject = community;
            }
            break;
        case Constants.ADD:
            if (AuthorizeConfiguration.canItemAdminPerformBitstreamCreation())
            {
                adminObject = item;
            }
            else if (AuthorizeConfiguration
                    .canCollectionAdminPerformBitstreamCreation())
            {
                adminObject = collection;
            }
            else if (AuthorizeConfiguration
                    .canCommunityAdminPerformBitstreamCreation())
            {
                adminObject = community;
            }
            break;

        default:
            adminObject = bundle;
            break;
        }
        return adminObject;
    }

    @Override
    public DSpaceObject getParentObject(Context context, Bundle bundle) throws SQLException {
        List<Item> items = bundle.getItems();
        if(CollectionUtils.isNotEmpty(items))
        {
            return items.iterator().next();
        }else{
            return null;
        }
    }

    @Override
    public void updateLastModified(Context context, Bundle dso) {
        //No implemented for bundle
    }

    @Override
    public void update(Context context, Bundle bundle) throws SQLException, AuthorizeException {
                // Check authorisation
        //AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        log.info(LogManager.getHeader(context, "update_bundle", "bundle_id="
                + bundle.getID()));

        bundleDAO.save(context, bundle);

        if (bundle.isModified() || bundle.isMetadataModified())
        {
            if(bundle.isMetadataModified()){
                context.addEvent(new Event(Event.MODIFY_METADATA, bundle.getType(), bundle.getID(), bundle.getDetails(), getIdentifiers(context, bundle)));
            }
            context.addEvent(new Event(Event.MODIFY, Constants.BUNDLE, bundle.getID(),
                    null, getIdentifiers(context, bundle)));
            bundle.clearModified();
            bundle.clearDetails();
        }
    }

    @Override
    public void delete(Context context, Bundle bundle) throws SQLException, AuthorizeException, IOException {
        log.info(LogManager.getHeader(context, "delete_bundle", "bundle_id="
                + bundle.getID()));

        context.addEvent(new Event(Event.DELETE, Constants.BUNDLE, bundle.getID(),
                bundle.getName(), getIdentifiers(context, bundle)));

        // Remove bitstreams
        Iterator<BundleBitstream> bundleBitstreams = bundle.getBitstreams().iterator();
        while (bundleBitstreams.hasNext()) {
            BundleBitstream bundleBitstream = bundleBitstreams.next();
            bundleBitstreams.remove();
            removeBitstream(context, bundle, bundleBitstream.getBitstream());
            bundleBitstreamDAO.delete(context, bundleBitstream);
        }

        Iterator<Item> items = bundle.getItems().iterator();
        while (items.hasNext()) {
            Item item = items.next();
            item.removeBundle(bundle);
        }


        // remove our authorization policies
        authorizeService.removeAllPolicies(context, bundle);

        deleteMetadata(context, bundle);

        // Remove ourself
        bundleDAO.delete(context, bundle);
    }

    @Override
    public Bundle findByIdOrLegacyId(Context context, String id) throws SQLException {
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
    public Bundle findByLegacyId(Context context, int id) throws SQLException {
        return bundleDAO.findByLegacyId(context, id, Bundle.class);
    }
}

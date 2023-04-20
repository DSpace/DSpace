/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.core.Constants.ADD;
import static org.dspace.core.Constants.REMOVE;
import static org.dspace.core.Constants.WRITE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Bundle object.
 * This class is responsible for all business logic calls for the Bundle object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BundleServiceImpl extends DSpaceObjectServiceImpl<Bundle> implements BundleService {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(Bundle.class);

    @Autowired(required = true)
    protected BundleDAO bundleDAO;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    protected BundleServiceImpl() {
        super();
    }

    @Override
    public Bundle find(Context context, UUID id) throws SQLException {
        // First check the cache
        Bundle bundle = bundleDAO.findByID(context, Bundle.class, id);
        if (bundle == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_bundle",
                                               "not_found,bundle_id=" + id));
            }

            return null;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_bundle",
                                               "bundle_id=" + id));
            }

            return bundle;
        }
    }

    @Override
    public Bundle create(Context context, Item item, String name) throws SQLException, AuthorizeException {
        if (StringUtils.isBlank(name)) {
            throw new SQLException("Bundle must be created with non-null name");
        }
        authorizeService.authorizeAction(context, item, Constants.ADD);


        // Create a table row
        Bundle bundle = bundleDAO.create(context, new Bundle());
        bundle.setName(context, name);
        itemService.addBundle(context, item, bundle);
        if (!bundle.getItems().contains(item)) {
            bundle.addItem(item);
        }


        log.info(LogHelper.getHeader(context, "create_bundle", "bundle_id="
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

        for (Bitstream bitstream : bundle.getBitstreams()) {
            if (name.equals(bitstream.getName())) {
                target = bitstream;
                break;
            }
        }

        return target;
    }

    @Override
    public void addBitstream(Context context, Bundle bundle, Bitstream bitstream)
        throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.ADD);

        log.info(LogHelper.getHeader(context, "add_bitstream", "bundle_id="
            + bundle.getID() + ",bitstream_id=" + bitstream.getID()));

        // First check that the bitstream isn't already in the list
        List<Bitstream> bitstreams = bundle.getBitstreams();
        int topOrder = 0;
        // First check that the bitstream isn't already in the list
        for (Bitstream bs : bitstreams) {
            if (bitstream.getID().equals(bs.getID())) {
                // Bitstream is already there; no change
                return;
            }
        }

        // Ensure that the last modified from the item is triggered !
        Item owningItem = (Item) getParentObject(context, bundle);
        if (owningItem != null) {
            itemService.updateLastModified(context, owningItem);
            itemService.update(context, owningItem);
        }

        bundle.addBitstream(bitstream);
        // If a bitstream is moved from one bundle to another it may be temporarily flagged as deleted
        // (when removed from the original bundle)
        if (bitstream.isDeleted()) {
            bitstream.setDeleted(false);
        }
        bitstream.getBundles().add(bundle);


        context.addEvent(new Event(Event.ADD, Constants.BUNDLE, bundle.getID(),
                                   Constants.BITSTREAM, bitstream.getID(), String.valueOf(bitstream.getSequenceID()),
                                   getIdentifiers(context, bundle)));

        // copy authorization policies from bundle to bitstream
        // FIXME: multiple inclusion is affected by this...
        authorizeService.inheritPolicies(context, bundle, bitstream);
        bitstreamService.update(context, bitstream);
    }

    @Override
    public void removeBitstream(Context context, Bundle bundle, Bitstream bitstream)
        throws AuthorizeException, SQLException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.REMOVE);

        log.info(LogHelper.getHeader(context, "remove_bitstream",
                                      "bundle_id=" + bundle.getID() + ",bitstream_id=" + bitstream.getID()));


        context.addEvent(new Event(Event.REMOVE, Constants.BUNDLE, bundle.getID(),
                                   Constants.BITSTREAM, bitstream.getID(), String.valueOf(bitstream.getSequenceID()),
                                   getIdentifiers(context, bundle)));

        //Ensure that the last modified from the item is triggered !
        Item owningItem = (Item) getParentObject(context, bundle);
        if (owningItem != null) {
            itemService.updateLastModified(context, owningItem);
            itemService.update(context, owningItem);
        }

        // In the event that the bitstream to remove is actually
        // the primary bitstream, be sure to unset the primary
        // bitstream.
        if (bitstream.equals(bundle.getPrimaryBitstream())) {
            bundle.unsetPrimaryBitstreamID();
        }

        // Check if our bitstream is part of a single or no bundle.
        // Bitstream.getBundles() may be empty (the delete() method clears
        // the bundles). We should not delete the bitstream, if it is used
        // in another bundle, instead we just remove the link between bitstream
        // and this bundle.
        if (bitstream.getBundles().size() <= 1) {
            // We don't need to remove the link between bundle & bitstream,
            // this will be handled in the delete() method.
            bitstreamService.delete(context, bitstream);
        } else {
            bundle.removeBitstream(bitstream);
            bitstream.getBundles().remove(bundle);
        }
    }

    @Override
    public void inheritCollectionDefaultPolicies(Context context, Bundle bundle, Collection collection)
        throws SQLException, AuthorizeException {
        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, collection,
                                                                                 Constants.DEFAULT_BITSTREAM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        Iterator<ResourcePolicy> i = policies.iterator();

        if (!i.hasNext()) {
            throw new java.sql.SQLException("Collection " + collection.getID()
                                                + " has no default bitstream READ policies");
        }

        List<ResourcePolicy> newPolicies = new ArrayList<ResourcePolicy>();
        while (i.hasNext()) {
            ResourcePolicy rp = resourcePolicyService.clone(context, i.next());
            rp.setAction(Constants.READ);
            newPolicies.add(rp);
        }

        replaceAllBitstreamPolicies(context, bundle, newPolicies);
    }

    @Override
    public void replaceAllBitstreamPolicies(Context context, Bundle bundle, List<ResourcePolicy> newpolicies)
        throws SQLException, AuthorizeException {
        List<Bitstream> bitstreams = bundle.getBitstreams();
        if (CollectionUtils.isNotEmpty(bitstreams)) {
            for (Bitstream bs : bitstreams) {
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
        List<Bitstream> bitstreams = bundle.getBitstreams();
        if (CollectionUtils.isNotEmpty(bitstreams)) {
            for (Bitstream bs : bitstreams) {
                list.addAll(authorizeService.getPolicies(context, bs));
            }
        }
        return list;
    }

    @Override
    public List<ResourcePolicy> getBundlePolicies(Context context, Bundle bundle) throws SQLException {
        return authorizeService.getPolicies(context, bundle);
    }

    @Override
    public void updateBitstreamOrder(Context context, Bundle bundle, int from, int to)
            throws AuthorizeException, SQLException {
        List<Bitstream> bitstreams = bundle.getBitstreams();
        if (bitstreams.size() < 1 || from >= bitstreams.size() || to >= bitstreams.size() || from < 0 || to < 0) {
            throw new IllegalArgumentException(
                    "Invalid 'from' and 'to' arguments supplied for moving a bitstream within bundle " +
                            bundle.getID() + ". from: " + from + "; to: " + to
            );
        }
        List<UUID> bitstreamIds = new LinkedList<>();
        for (Bitstream bitstream : bitstreams) {
            bitstreamIds.add(bitstream.getID());
        }
        if (from < to) {
            bitstreamIds.add(to + 1, bitstreamIds.get(from));
            bitstreamIds.remove(from);
        } else {
            bitstreamIds.add(to, bitstreamIds.get(from));
            bitstreamIds.remove(from + 1);
        }
        setOrder(context, bundle, bitstreamIds.toArray(new UUID[bitstreamIds.size()]));
    }

    @Override
    public void moveBitstreamToBundle(Context context, Bundle targetBundle, Bitstream bitstream)
            throws SQLException, AuthorizeException, IOException {
        List<Bundle> bundles = new LinkedList<>();
        bundles.addAll(bitstream.getBundles());

        if (hasSufficientMovePermissions(context, bundles, targetBundle)) {
            this.addBitstream(context, targetBundle, bitstream);
            this.update(context, targetBundle);
            for (Bundle bundle : bundles) {
                this.removeBitstream(context, bundle, bitstream);
                this.update(context, bundle);
            }
        }
    }


    /**
     * Verifies if the context (user) has sufficient rights to the bundles in order to move a bitstream
     *
     * @param context      The context
     * @param bundles      The current bundles in which the bitstream resides
     * @param targetBundle The target bundle
     * @return true when the context has sufficient rights
     * @throws AuthorizeException When one of the necessary rights is not present
     */
    private boolean hasSufficientMovePermissions(final Context context, final List<Bundle> bundles,
                                                 final Bundle targetBundle) throws SQLException, AuthorizeException {
        for (Bundle bundle : bundles) {
            if (!authorizeService.authorizeActionBoolean(context, bundle, WRITE) || !authorizeService
                    .authorizeActionBoolean(context, bundle, REMOVE)) {
                throw new AuthorizeException(
                        "The current user does not have WRITE and REMOVE access to the current bundle: " + bundle
                                .getID());
            }
        }
        if (!authorizeService.authorizeActionBoolean(context, targetBundle, WRITE) || !authorizeService
                .authorizeActionBoolean(context, targetBundle, ADD)) {
            throw new AuthorizeException(
                    "The current user does not have WRITE and ADD access to the target bundle: " + targetBundle
                            .getID());
        }
        for (Item item : targetBundle.getItems()) {
            if (!authorizeService.authorizeActionBoolean(context, item, WRITE)) {
                throw new AuthorizeException(
                        "The current user does not have WRITE access to the target bundle's item: " + item.getID());
            }
        }
        return true;
    }

    @Override
    public void setOrder(Context context, Bundle bundle, UUID[] bitstreamIds) throws AuthorizeException, SQLException {
        authorizeService.authorizeAction(context, bundle, Constants.WRITE);

        List<Bitstream> currentBitstreams = bundle.getBitstreams();
        List<Bitstream> updatedBitstreams = new ArrayList<Bitstream>();

        // Loop through and ensure these Bitstream IDs are all valid. Add them to list of updatedBitstreams.
        for (int i = 0; i < bitstreamIds.length; i++) {
            UUID bitstreamId = bitstreamIds[i];
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);

            // If we have an invalid Bitstream ID, just ignore it, but log a warning
            if (bitstream == null) {
                //This should never occur but just in case
                log.warn(LogHelper.getHeader(context, "Invalid bitstream id while changing bitstream order",
                                              "Bundle: " + bundle.getID() + ", bitstream id: " + bitstreamId));
                continue;
            }

            // If we have a Bitstream not in the current list, log a warning & exit immediately
            if (!currentBitstreams.contains(bitstream)) {
                log.warn(LogHelper.getHeader(context,
                                              "Encountered a bitstream not in this bundle while changing bitstream " +
                                                  "order. Bitstream order will not be changed.",
                                              "Bundle: " + bundle.getID() + ", bitstream id: " + bitstreamId));
                return;
            }
            updatedBitstreams.add(bitstream);
        }

        // If our lists are different sizes, exit immediately
        if (updatedBitstreams.size() != currentBitstreams.size()) {
            log.warn(LogHelper.getHeader(context,
                                          "Size of old list and new list do not match. Bitstream order will not be " +
                                              "changed.",
                                          "Bundle: " + bundle.getID()));
            return;
        }

        // As long as the order has changed, update it
        if (CollectionUtils.isNotEmpty(updatedBitstreams) && !updatedBitstreams.equals(currentBitstreams)) {
            //First clear out the existing list of bitstreams
            bundle.clearBitstreams();

            // Now add them back in the proper order
            for (Bitstream bitstream : updatedBitstreams) {
                bitstream.getBundles().remove(bundle);
                bundle.addBitstream(bitstream);
                bitstream.getBundles().add(bundle);
                bitstreamService.update(context, bitstream);
            }

            //The order of the bitstreams has changed, ensure that we update the last modified of our item
            Item owningItem = (Item) getParentObject(context, bundle);
            if (owningItem != null) {
                itemService.updateLastModified(context, owningItem);
                itemService.update(context, owningItem);

            }
        }
    }

    @Override
    public DSpaceObject getAdminObject(Context context, Bundle bundle, int action) throws SQLException {
        DSpaceObject adminObject = null;
        Item item = (Item) getParentObject(context, bundle);
        Collection collection = null;
        Community community = null;
        if (item != null) {
            collection = item.getOwningCollection();
            if (collection != null) {
                community = collection.getCommunities().get(0);
            }
        }
        switch (action) {
            case Constants.REMOVE:
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamDeletion()) {
                    adminObject = item;
                } else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamDeletion()) {
                    adminObject = collection;
                } else if (AuthorizeConfiguration
                    .canCommunityAdminPerformBitstreamDeletion()) {
                    adminObject = community;
                }
                break;
            case Constants.ADD:
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamCreation()) {
                    adminObject = item;
                } else if (AuthorizeConfiguration
                    .canCollectionAdminPerformBitstreamCreation()) {
                    adminObject = collection;
                } else if (AuthorizeConfiguration
                    .canCommunityAdminPerformBitstreamCreation()) {
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
        if (CollectionUtils.isNotEmpty(items)) {
            return items.iterator().next();
        } else {
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
        log.info(LogHelper.getHeader(context, "update_bundle", "bundle_id="
            + bundle.getID()));

        super.update(context, bundle);
        bundleDAO.save(context, bundle);

        if (bundle.isModified() || bundle.isMetadataModified()) {
            if (bundle.isMetadataModified()) {
                context.addEvent(new Event(Event.MODIFY_METADATA, bundle.getType(), bundle.getID(), bundle.getDetails(),
                                           getIdentifiers(context, bundle)));
            }
            context.addEvent(new Event(Event.MODIFY, Constants.BUNDLE, bundle.getID(),
                                       null, getIdentifiers(context, bundle)));
            bundle.clearModified();
            bundle.clearDetails();
        }
    }

    @Override
    public void delete(Context context, Bundle bundle) throws SQLException, AuthorizeException, IOException {
        log.info(LogHelper.getHeader(context, "delete_bundle", "bundle_id="
            + bundle.getID()));

        authorizeService.authorizeAction(context, bundle, Constants.DELETE);

        context.addEvent(new Event(Event.DELETE, Constants.BUNDLE, bundle.getID(),
                                   bundle.getName(), getIdentifiers(context, bundle)));

        // Remove bitstreams
        List<Bitstream> bitstreams = bundle.getBitstreams();
        for (Bitstream bitstream : bitstreams) {
            removeBitstream(context, bundle, bitstream);
        }
        bundle.clearBitstreams();

        List<Item> items = new LinkedList<>(bundle.getItems());
        bundle.getItems().clear();
        for (Item item : items) {
            item.removeBundle(bundle);
        }

        // Remove ourself
        bundleDAO.delete(context, bundle);
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.BUNDLE;
    }

    @Override
    public Bundle findByIdOrLegacyId(Context context, String id) throws SQLException {
        if (StringUtils.isNumeric(id)) {
            return findByLegacyId(context, Integer.parseInt(id));
        } else {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public Bundle findByLegacyId(Context context, int id) throws SQLException {
        return bundleDAO.findByLegacyId(context, id, Bundle.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return bundleDAO.countRows(context);
    }
}

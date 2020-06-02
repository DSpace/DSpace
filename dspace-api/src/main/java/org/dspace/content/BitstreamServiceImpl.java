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
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for the Bitstream object.
 * This class is responsible for all business logic calls for the Bitstream object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamServiceImpl extends DSpaceObjectServiceImpl<Bitstream> implements BitstreamService {

    /** log4j logger */
    private static Logger log = Logger.getLogger(BitstreamServiceImpl.class);


    @Autowired(required = true)
    protected BitstreamDAO bitstreamDAO;
    @Autowired(required = true)
    protected ItemService itemService;


    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected BitstreamStorageService bitstreamStorageService;

    protected BitstreamServiceImpl()
    {
        super();
    }

    @Override
    public Bitstream find(Context context, UUID id) throws SQLException {
        Bitstream bitstream = bitstreamDAO.findByID(context, Bitstream.class, id);

        if (bitstream == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_bitstream",
                        "not_found,bitstream_id=" + id));
            }

            return null;
        }

                // not null, return Bitstream
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_bitstream",
                    "bitstream_id=" + id));
        }

        return bitstream;
    }

    @Override
    public List<Bitstream> findAll(Context context) throws SQLException
    {
        return bitstreamDAO.findAll(context, Bitstream.class);
    }

    @Override
    public Bitstream clone(Context context, Bitstream bitstream)
            throws SQLException 
    {
        // Create a new bitstream with a new ID.
        Bitstream clonedBitstream = bitstreamDAO.create(context, new Bitstream());
        // Set the internal identifier, file size, checksum, and 
        // checksum algorithm as same as the given bitstream. 
        clonedBitstream.setInternalId(bitstream.getInternalId());
        clonedBitstream.setSizeBytes(bitstream.getSizeBytes());
        clonedBitstream.setChecksum(bitstream.getChecksum());
        clonedBitstream.setChecksumAlgorithm(bitstream.getChecksumAlgorithm());
        clonedBitstream.setFormat(bitstream.getBitstreamFormat());

        try 
        {
            //Update our bitstream but turn off the authorization system since permissions haven't been set at this point in time.
            context.turnOffAuthorisationSystem();
            update(context, clonedBitstream);
        } 
        catch (AuthorizeException e) 
        {
            log.error(e);
            //Can never happen since we turn off authorization before we update
        } 
        finally 
        {
            context.restoreAuthSystemState();
        }
        return clonedBitstream;
    }
    
    @Override
    public Bitstream create(Context context, InputStream is) throws IOException, SQLException {
        // Store the bits
        UUID bitstreamID = bitstreamStorageService.store(context, bitstreamDAO.create(context, new Bitstream()), is);

        log.info(LogManager.getHeader(context, "create_bitstream",
                "bitstream_id=" + bitstreamID));

        // Set the format to "unknown"
        Bitstream bitstream = find(context, bitstreamID);
        setFormat(context, bitstream, null);

        context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM, bitstreamID, null, getIdentifiers(context, bitstream)));

        return bitstream;
    }

    @Override
    public Bitstream create(Context context, Bundle bundle, InputStream is) throws IOException, SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.ADD);

        Bitstream b = create(context, is);
        bundleService.addBitstream(context, bundle, b);
        return b;
    }

    @Override
    public Bitstream register(Context context, Bundle bundle, int assetstore, String bitstreamPath) throws IOException, SQLException, AuthorizeException {
        // check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.ADD);

        Bitstream bitstream = register(context, assetstore, bitstreamPath);

        bundleService.addBitstream(context, bundle, bitstream);
        return bitstream;
    }

    /**
     * Register a new bitstream, with a new ID.  The checksum and file size
     * are calculated.  This method is not public, and does not check
     * authorisation; other methods such as Bundle.createBitstream() will
     * check authorisation.  The newly created bitstream has the "unknown"
     * format.
     *
     * @param  context DSpace context object
     * @param assetstore corresponds to an assetstore in dspace.cfg
     * @param bitstreamPath the path and filename relative to the assetstore
     * @return  the newly registered bitstream
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public Bitstream register(Context context,
    		int assetstore, String bitstreamPath)
            throws IOException, SQLException, AuthorizeException {
        // Store the bits
        Bitstream bitstream = bitstreamDAO.create(context, new Bitstream());
        bitstreamStorageService.register(
                context, bitstream, assetstore, bitstreamPath);

        log.info(LogManager.getHeader(context,
            "create_bitstream",
            "bitstream_id=" + bitstream.getID()));

        // Set the format to "unknown"
        setFormat(context, bitstream, null);

        context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM,
                bitstream.getID(), "REGISTER", getIdentifiers(context, bitstream)));

        return bitstream;
    }

    @Override
    public void setUserFormatDescription(Context context, Bitstream bitstream, String desc) throws SQLException {
        setFormat(context,bitstream,  null);
        setMetadataSingleValue(context, bitstream, MetadataSchema.DC_SCHEMA, "format", null, null, desc);
    }

    @Override
    public String getFormatDescription(Context context, Bitstream bitstream) throws SQLException
    {
        if (bitstream.getFormat(context).getShortDescription().equals("Unknown"))
        {
            // Get user description if there is one
            String desc = bitstream.getUserFormatDescription();

            if (desc == null)
            {
                return "Unknown";
            }

            return desc;
        }

        // not null or Unknown
        return bitstream.getFormat(context).getShortDescription();
    }

    @Override
    public void setFormat(Context context, Bitstream bitstream, BitstreamFormat bitstreamFormat) throws SQLException {
                // FIXME: Would be better if this didn't throw an SQLException,
        // but we need to find the unknown format!
        if (bitstreamFormat == null)
        {
            // Use "Unknown" format
            bitstreamFormat = bitstreamFormatService.findUnknown(context);
        }

        // Remove user type description
        clearMetadata(context, bitstream, MetadataSchema.DC_SCHEMA,"format",null, Item.ANY);

        // Update the ID in the table row
        bitstream.setFormat(bitstreamFormat);
    }

    @Override
    public void update(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, bitstream, Constants.WRITE);

        log.info(LogManager.getHeader(context, "update_bitstream",
                "bitstream_id=" + bitstream.getID()));
        super.update(context, bitstream);
        if (bitstream.isModified())
        {
            context.addEvent(new Event(Event.MODIFY, Constants.BITSTREAM, bitstream.getID(), null, getIdentifiers(context, bitstream)));
            bitstream.setModified();
        }
        if (bitstream.isMetadataModified())
        {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.BITSTREAM, bitstream.getID(), bitstream.getDetails(), getIdentifiers(context, bitstream)));
            bitstream.clearModified();
            bitstream.clearDetails();
        }

        bitstreamDAO.save(context, bitstream);
    }

    @Override
    public void delete(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {

        // changed to a check on delete
        // Check authorisation
        authorizeService.authorizeAction(context, bitstream, Constants.DELETE);
        log.info(LogManager.getHeader(context, "delete_bitstream",
                "bitstream_id=" + bitstream.getID()));

        context.addEvent(new Event(Event.DELETE, Constants.BITSTREAM, bitstream.getID(),
                String.valueOf(bitstream.getSequenceID()), getIdentifiers(context, bitstream)));

        // Remove bitstream itself
        bitstream.setDeleted(true);
        update(context, bitstream);

        //Remove our bitstream from all our bundles
        final List<Bundle> bundles = bitstream.getBundles();
        for (Bundle bundle : bundles) {
            bundle.removeBitstream(bitstream);
        }

        //Remove all bundles from the bitstream object, clearing the connection in 2 ways
        bundles.clear();

        // Remove policies only after the bitstream has been updated (otherwise the current user has not WRITE rights)
        authorizeService.removeAllPolicies(context, bitstream);
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.BITSTREAM;
    }

    @Override
    public InputStream retrieve(Context context, Bitstream bitstream) throws IOException, SQLException, AuthorizeException {
        // Maybe should return AuthorizeException??
        authorizeService.authorizeAction(context, bitstream, Constants.READ);

        return bitstreamStorageService.retrieve(context, bitstream);
    }

    @Override
    public boolean isRegisteredBitstream(Bitstream bitstream) {
        return bitstreamStorageService.isRegisteredBitstream(bitstream.getInternalId());
    }

    @Override
    public DSpaceObject getParentObject(Context context, Bitstream bitstream) throws SQLException {
        List<Bundle> bundles = bitstream.getBundles();
        if (CollectionUtils.isNotEmpty(bundles))
        {
            // the ADMIN action is not allowed on Bundle object so skip to the item
            Item item = (Item) bundleService.getParentObject(context, bundles.iterator().next());
            if (item != null)
            {
                return item;
            }
            else
            {
                return null;
            }
        }
        else
        if(bitstream.getCommunity() != null)
        {
            return bitstream.getCommunity();
        }else
        if(bitstream.getCollection() != null)
        {
            return bitstream.getCollection();
        }
        return null;
    }

    @Override
    public void updateLastModified(Context context, Bitstream bitstream) {
        //Also fire a modified event since the bitstream HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.BITSTREAM, bitstream.getID(), null, getIdentifiers(context, bitstream)));
    }

    @Override
    public List<Bitstream> findDeletedBitstreams(Context context) throws SQLException {
        return bitstreamDAO.findDeletedBitstreams(context);
    }

    @Override
    public void expunge(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {
        authorizeService.authorizeAction(context, bitstream, Constants.DELETE);
        if(!bitstream.isDeleted())
        {
            throw new IllegalStateException("Bitstream must be deleted before it can be removed from the database");
        }
        bitstreamDAO.delete(context, bitstream);
    }

    @Override
    public List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException {
        return bitstreamDAO.findDuplicateInternalIdentifier(context, bitstream);
    }

    @Override
    public Iterator<Bitstream> getItemBitstreams(Context context, Item item) throws SQLException {
        return bitstreamDAO.findByItem(context, item);
    }


    @Override
    public Iterator<Bitstream> getCollectionBitstreams(Context context, Collection collection) throws SQLException {
        return bitstreamDAO.findByCollection(context, collection);

    }

    @Override
    public Iterator<Bitstream> getCommunityBitstreams(Context context, Community community) throws SQLException {
        return bitstreamDAO.findByCommunity(context, community);
    }

    @Override
    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException {
        return bitstreamDAO.findBitstreamsWithNoRecentChecksum(context);
    }

    @Override
    public Bitstream getBitstreamByName(Item item, String bundleName, String bitstreamName) throws SQLException {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = bundles.get(i);
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (int j = 0; j < bitstreams.size(); j++) {
                Bitstream bitstream = bitstreams.get(j);
                if(StringUtils.equals(bitstream.getName(), bitstreamName))
                {
                    return bitstream;
                }
            }
        }
        return null;
    }

    @Override
    public Bitstream getFirstBitstream(Item item, String bundleName) throws SQLException {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        if(CollectionUtils.isNotEmpty(bundles))
        {
            List<Bitstream> bitstreams = bundles.get(0).getBitstreams();
            if(CollectionUtils.isNotEmpty(bitstreams))
            {
                return bitstreams.get(0);
            }
        }
        return null;
    }

    @Override
    public BitstreamFormat getFormat(Context context, Bitstream bitstream) throws SQLException {
        if(bitstream.getBitstreamFormat() == null)
        {
            return bitstreamFormatService.findUnknown(context);
        }else{
            return bitstream.getBitstreamFormat();
        }
    }

    @Override
    public Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        return bitstreamDAO.findByStoreNumber(context, storeNumber);
    }

    @Override
    public Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        return bitstreamDAO.countByStoreNumber(context, storeNumber);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return bitstreamDAO.countRows(context);
    }

    @Override
    public Bitstream findByIdOrLegacyId(Context context, String id) throws SQLException {
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
    public Bitstream findByLegacyId(Context context, int id) throws SQLException {
        return bitstreamDAO.findByLegacyId(context, id, Bitstream.class);

    }

    @Override
    public int countDeletedBitstreams(Context context) throws SQLException {
        return bitstreamDAO.countDeleted(context);
    }

    @Override
    public int countBitstreamsWithoutPolicy(Context context) throws SQLException {
        return bitstreamDAO.countWithNoPolicy(context);
    }

    @Override
    public List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException {
        return bitstreamDAO.getNotReferencedBitstreams(context);
    }
}

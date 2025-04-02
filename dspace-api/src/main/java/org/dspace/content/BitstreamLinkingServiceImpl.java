/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.BitstreamLinkingService;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the BitstreamLinkingService
 * This class is responsible for providing metadata to bitstreams that are cloned in order to track which bitstreams
 * are copies, original, replacement, or replaced by.
 *
 * @author Nathan Buckingham at atmire.com
 */
public class BitstreamLinkingServiceImpl implements BitstreamLinkingService {

    @Autowired
    BitstreamService bitstreamService;

    public static final String DSPACE = "dspace";
    public static final String BITSTREAM = "bitstream";
    public static final String HAS_COPIES = "hasCopies";
    public static final String IS_COPY_OF = "isCopyOf";
    public static final String IS_REPLACED_BY = "isReplacedBy";
    public static final String IS_REPLACEMENT_OF = "isReplacementOf";

    /**
     * log4j logger
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Override
    public void cloneMetadata(Context context, Bitstream bitstream, Bitstream clone) throws SQLException,
            AuthorizeException {
        registerBitstreams(context, bitstream, clone);
        skipBitstreamMetadataThenAdd(context, bitstream, clone);
    }

    @Override
    public void replaceMetadata(Context context, Bitstream bitstream, Bitstream replacedBy) throws SQLException,
            AuthorizeException {
        registerReplacementBitstream(context, bitstream, replacedBy);
        skipBitstreamMetadataThenAdd(context, bitstream, replacedBy);
    }

    @Override
    public void registerBitstreams(Context context, Bitstream oldCopy,
                                   Bitstream newCopy) throws SQLException, AuthorizeException {
        bitstreamService.addMetadata(context, oldCopy, DSPACE, BITSTREAM,
                HAS_COPIES, null, newCopy.getID().toString());
        bitstreamService.addMetadata(context, newCopy, DSPACE, BITSTREAM,
                IS_COPY_OF, null, oldCopy.getID().toString());
        bitstreamService.update(context, oldCopy);
    }

    @Override
    public void registerReplacementBitstream(Context context, Bitstream oldCopy,
                                             Bitstream replacementCopy) throws SQLException, AuthorizeException {
        bitstreamService.addMetadata(context, oldCopy, DSPACE, BITSTREAM,
                IS_REPLACED_BY, null, replacementCopy.getID().toString());
        bitstreamService.addMetadata(context, replacementCopy, DSPACE, BITSTREAM,
                IS_REPLACEMENT_OF, null, oldCopy.getID().toString());
        bitstreamService.update(context, oldCopy);
    }

    @Override
    public List<Bitstream> getCopies(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstreams(context, bitstream, HAS_COPIES);
    }

    @Override
    public Bitstream getOriginal(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstream(context, bitstream, IS_COPY_OF);
    }

    @Override
    public List<Bitstream> getReplacements(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstreams(context, bitstream, IS_REPLACED_BY);
    }

    @Override
    public Bitstream getOriginalReplacement(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstream(context, bitstream, IS_REPLACEMENT_OF);
    }

    /**
     * Gets the array of related bitstreams to the metadata given by qualifier.
     *
     * @param context Context
     * @param bitstream The bitstream to search from
     * @param qualifier The qualifier of the metadata key to search upon
     * @return The array of related bitstreams
     * @throws SQLException If bitstreamService.fine() fails to access the database
     */
    private List<Bitstream> getRelatedBitstreamArray(Context context, Bitstream bitstream, String qualifier)
            throws SQLException {
        List<Bitstream> bitstreams = new ArrayList<>();
        List<MetadataValue> uuids = bitstreamService.getMetadata(bitstream, DSPACE, BITSTREAM, qualifier, null);
        for (MetadataValue uuid : uuids) {
            bitstreams.add(bitstreamService.find(context, UUID.fromString(uuid.getValue())));
        }
        return bitstreams;
    }

    /**
     * Inner function that is used to get all related bitstreams according to a specific metadataField
     *
     * @param context Context
     * @param bitstream The bitstream to search from
     * @param qualifier The qualifier of the metadata key to search upon
     *
     * @return List<Bitstream> of bitstreams that were found using the uuids found in the given metadatafield.
     * @throws SQLException If bitstreamService.find() fails to access the database
     */
    protected List<Bitstream> getRelatedBitstreams(Context context, Bitstream bitstream,
                                                   String qualifier) throws SQLException {
        return getRelatedBitstreamArray(context, bitstream, qualifier);
    }

    /**
     * Inner function that is used to get one related bitstreams according to a specific metadataField
     *
     * @param context Context
     * @param bitstream The bitstream to search from
     * @param qualifier The qualifier of the metadata key to search upon
     *
     * @return The bitstream that was found using the uuids found in the given metadatafield.
     * @throws SQLException If bitstreamService.find() fails to access the database
     */
    protected Bitstream getRelatedBitstream(Context context, Bitstream bitstream,
                                            String qualifier) throws SQLException {
        List<Bitstream> bitstreams = getRelatedBitstreamArray(context, bitstream, qualifier);
        if (bitstreams.size() > 1) {
            log.warn("Related bitstream for: " + qualifier + " should only contain one bitstream, database " +
                    "errors may be present if this is the case");
        }
        return !bitstreams.isEmpty() ? bitstreams.get(0) : null;
    }

    /**
     * Clones the metadata to the new bitstream without taking any bitstream linking data with it
     *
     * @param context Dspace Context
     * @param bitstream DSpace original Bitstream
     * @param clone Dspace
     * @throws SQLException
     */
    private void skipBitstreamMetadataThenAdd(Context context, Bitstream bitstream, Bitstream clone)
            throws SQLException {
        List<MetadataValue> metadataValues = bitstreamService.getMetadata(bitstream, Item.ANY, Item.ANY, Item.ANY,
                Item.ANY);

        for (MetadataValue metadataValue : metadataValues) {
            if (metadataValue.getMetadataField().toString().equals(DSPACE + "_" + BITSTREAM + "_" + HAS_COPIES) ||
                metadataValue.getMetadataField().toString().equals(DSPACE + "_" + BITSTREAM + "_" + IS_COPY_OF) ||
                metadataValue.getMetadataField().toString().equals(DSPACE + "_" + BITSTREAM + "_" + IS_REPLACED_BY) ||
                metadataValue.getMetadataField().toString().equals(DSPACE + "_" + BITSTREAM + "_" + IS_REPLACEMENT_OF)
            ) {
                continue;
            }
            bitstreamService.addMetadata(context, clone, metadataValue.getMetadataField(),
                    metadataValue.getLanguage(), metadataValue.getValue(), metadataValue.getAuthority(),
                    metadataValue.getConfidence());
        }
    }
}

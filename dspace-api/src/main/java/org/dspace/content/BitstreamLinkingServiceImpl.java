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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.BitstreamLinkingService;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the BitstreamLinkingService
 * This class is responsible for providing links between bitstreams via the versioning service
 *
 * @author Nathan Buckingham at atmire.com
 */
public class BitstreamLinkingServiceImpl implements BitstreamLinkingService {

    @Autowired
    BitstreamService bitstreamService;

    public static final String HAS_COPIES = "dspace.bitstream.hasCopies";
    public static final String IS_COPY_OF = "dspace.bitstream.isCopyOf";
    public static final String IS_REPLACED_BY = "dspace.bitstream.isReplacedBy";
    public static final String IS_REPLACEMENT_OF = "dspace.bitstream.isReplacementOf";


    @Override
    public void registerBitstreams(Context context, Bitstream oldCopy,
                                   Bitstream newCopy) throws SQLException, AuthorizeException {
        bitstreamService.addMetadata(context, oldCopy, "dspace", "bitstream",
                "hasCopies", null, newCopy.getID().toString());
        bitstreamService.addMetadata(context, newCopy, "dspace", "bitstream",
                "isCopyOf", null, oldCopy.getID().toString());
        bitstreamService.update(context, oldCopy);
    }

    @Override
    public void registerReplacementBitstream(Context context, Bitstream oldCopy,
                                             Bitstream replacementCopy) throws SQLException, AuthorizeException {
        bitstreamService.addMetadata(context, oldCopy, "dspace", "bitstream",
                "isReplacedBy", null, replacementCopy.getID().toString());
        bitstreamService.addMetadata(context, replacementCopy, "dspace", "bitstream",
                "isReplacementOf", null, oldCopy.getID().toString());
        bitstreamService.update(context, oldCopy);
    }

    @Override
    public List<Bitstream> getCopies(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstreams(context, bitstream, HAS_COPIES);
    }

    @Override
    public List<Bitstream> getOriginals(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstreams(context, bitstream, IS_COPY_OF);
    }

    @Override
    public List<Bitstream> getReplacements(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstreams(context, bitstream, IS_REPLACED_BY);
    }

    @Override
    public List<Bitstream> getOriginalReplacement(Context context, Bitstream bitstream) throws SQLException {
        return getRelatedBitstreams(context, bitstream, IS_REPLACEMENT_OF);
    }

    /**
     * Inner class that is used to get all related bitstreams according to a specific metadataField
     *
     * @param context Context
     * @param bitstream The bitstream to search from
     * @param metadataField The metadatafield 'schema.element.qualifier' that is then split to the bitstreamService to
     *                      find what we assume are UUIDS.
     * @return List<Bitstream> of bitstreams that were found using the uuids found in the given metadatafield.
     * @throws SQLException If bitstreamService.find() fails to access the database
     */
    protected List<Bitstream> getRelatedBitstreams(Context context, Bitstream bitstream, String metadataField)
            throws SQLException {
        String[] metadataFields = metadataField.split("\\.");
        List<Bitstream> bitstreams = new ArrayList<>();
        List<MetadataValue> uuids = bitstreamService.getMetadata(bitstream, metadataFields[0], metadataFields[1],
                metadataFields[2], null);
        for (MetadataValue uuid : uuids) {
            bitstreams.add(bitstreamService.find(context, UUID.fromString(uuid.getValue())));
        }
        return bitstreams;

    }
}

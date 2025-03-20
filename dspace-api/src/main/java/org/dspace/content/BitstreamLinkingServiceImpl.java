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
        List<Bitstream> bitstreamCopies = new ArrayList<>();
        List<MetadataValue> uuids = bitstreamService.getMetadata(bitstream, "dspace", "bitstream",
                "hasCopies", null);
        for (MetadataValue uuid : uuids) {
            bitstreamCopies.add(bitstreamService.find(context, UUID.fromString(uuid.getValue())));
        }
        return bitstreamCopies;
    }

    @Override
    public List<Bitstream> getOriginals(Context context, Bitstream bitstream) throws SQLException {
        List<Bitstream> bitstreamOriginals = new ArrayList<>();
        List<MetadataValue> uuids = bitstreamService.getMetadata(bitstream, "dspace", "bitstream",
                "isCopyOf", null);
        for (MetadataValue uuid : uuids) {
            bitstreamOriginals.add(bitstreamService.find(context, UUID.fromString(uuid.getValue())));
        }
        return bitstreamOriginals;
    }

    @Override
    public List<Bitstream> getReplacements(Context context, Bitstream bitstream) throws SQLException {
        List<Bitstream> bitstreamReplacements = new ArrayList<>();
        List<MetadataValue> uuids = bitstreamService.getMetadata(bitstream, "dspace", "bitstream",
                "isReplacedBy", null);
        for (MetadataValue uuid : uuids) {
            bitstreamReplacements.add(bitstreamService.find(context, UUID.fromString(uuid.getValue())));
        }
        return bitstreamReplacements;
    }

    @Override
    public List<Bitstream> getOriginalReplacement(Context context, Bitstream bitstream) throws SQLException {
        List<Bitstream> bitstreamOriginalReplacement = new ArrayList<>();
        List<MetadataValue> uuids = bitstreamService.getMetadata(bitstream, "dspace", "bitstream",
                "isReplacementOf", null);
        for (MetadataValue uuid : uuids) {
            bitstreamOriginalReplacement.add(bitstreamService.find(context, UUID.fromString(uuid.getValue())));
        }
        return bitstreamOriginalReplacement;
    }
}

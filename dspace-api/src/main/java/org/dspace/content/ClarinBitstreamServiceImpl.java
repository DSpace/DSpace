/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.clarin.ClarinBitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.dspace.storage.bitstore.DSBitStoreService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service interface class for the Bitstream object created for Clarin-Dspace import.
 * Contains methods needed to import bitstream when dspace5 migrating to dspace7.
 * The implementation of this class is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
//If this class wants to catch the Bitstream protected constructor, it must be in this package!
public class ClarinBitstreamServiceImpl implements ClarinBitstreamService {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ClarinBitstreamServiceImpl.class);

    // Checksum algorithm
    private static final String CSA = "MD5";

    @Autowired
    private DSBitStoreService storeService;
    @Autowired
    protected BitstreamDAO bitstreamDAO;
    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    protected BundleService bundleService;
    @Autowired
    protected BitstreamService bitstreamService;

    protected ClarinBitstreamServiceImpl() {
    }

    @Override
    public Bitstream create(Context context, Bundle bundle) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an empty bitstream");
        }
        //create empty bitstream
        Bitstream bitstream = bitstreamDAO.create(context, new Bitstream());

        // Set the format to "unknown"
        bitstreamService.setFormat(context, bitstream, null);
        context.addEvent(
                new Event(Event.CREATE, Constants.BITSTREAM, bitstream.getID(),
                        null, bitstreamService.getIdentifiers(context, bitstream)));

        //add bitstream to bundle if the bundle is entered
        if (Objects.nonNull(bundle)) {
            bundleService.addBitstream(context, bundle, bitstream);
        }
        log.debug("Created new empty Bitstream with id: " + bitstream.getID());
        return bitstream;
    }

    @Override
    public boolean addExistingFile(Context context, Bitstream bitstream, Long expectedSizeBytes,
                                   String expectedCheckSum, String expectedChecksumAlgorithm)
            throws IOException, SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to add existing file to bitstream");
        }
        if (Objects.isNull(bitstream) || StringUtils.isBlank(bitstream.getInternalId())) {
            throw new IllegalStateException(
                    "Cannot add file to bitstream because it is entered incorrectly.");
        }
        //get file from assetstore based on internal_id
        //recalculate check fields
        storeService.put(bitstream, new ByteArrayInputStream(storeService.get(bitstream).readAllBytes()));
        //check that new calculated values match the expected values
        if (!valid(bitstream, expectedSizeBytes, expectedCheckSum, expectedChecksumAlgorithm)) {
            //an error occurred - expected and calculated values do not match
            //delete all created data
            bitstreamService.delete(context, bitstream);
            bitstreamService.expunge(context, bitstream);
            log.debug("Cannot add file with internal id: " +
                    bitstream.getInternalId() + " to bitstream with id: " + bitstream.getID()
                    + " because the validation is incorrectly.");
            return false;
        }
        bitstreamService.update(context, bitstream);
        return true;
    }

    /**
     * Validation control.
     * Control that expected values (method attributes) match with bitstream calculated values.
     * @param bitstream bitstream
     * @param expectedSizeBytes expected size bytes
     * @param expectedCheckSum expected checksum
     * @param expectedChecksumAlgorithm expected checksum algorithm
     * @return bitstream values match with expected values
     */
    private boolean valid(Bitstream bitstream, long expectedSizeBytes,
                                     String expectedCheckSum, String expectedChecksumAlgorithm) {
        return bitstream.getSizeBytes() == expectedSizeBytes && bitstream.getChecksum().equals(expectedCheckSum) &&
                bitstream.getChecksumAlgorithm().equals(expectedChecksumAlgorithm);
    }
}

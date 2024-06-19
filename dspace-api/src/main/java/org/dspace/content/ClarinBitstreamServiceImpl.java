/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.MapUtils;
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
import org.dspace.storage.bitstore.SyncBitstreamStorageServiceImpl;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
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
    private SyncBitstreamStorageServiceImpl syncBitstreamStorageService;
    @Autowired
    protected BitstreamDAO bitstreamDAO;
    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    protected BundleService bundleService;
    @Autowired
    protected BitstreamService bitstreamService;
    @Autowired
    private BitstreamStorageService bitstreamStorageService;

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
    public boolean validation(Context context, Bitstream bitstream)
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
        List<String> wantedMetadata = List.of("size_bytes", "checksum", "checksum_algorithm");
        Map<String, Object> receivedMetadata = syncBitstreamStorageService
                .getStore(syncBitstreamStorageService.whichStoreNumber(bitstream))
                .about(bitstream, wantedMetadata);
        //check that new calculated values match the expected values
        if (MapUtils.isEmpty(receivedMetadata) || !valid(bitstream, receivedMetadata)) {
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
     * Control that expected values (bitstream attributes) match with calculated values.
     * @param bitstream bitstream
     * @param checksumMap calculated values
     * @return bitstream values match with expected values
     */
    private boolean valid(Bitstream bitstream, Map<String, Object> checksumMap) {
        if (!checksumMap.containsKey("checksum") || !checksumMap.containsKey("checksum_algorithm") ||
                !checksumMap.containsKey("size_bytes")) {
            log.error("Cannot validate of bitstream with id: " + bitstream.getID() +
                    ", because there were no calculated all required fields.");
            return false;
        }
        return bitstream.getSizeBytes() == Long.valueOf(checksumMap.get("size_bytes").toString()) &&
                bitstream.getChecksum().equals(checksumMap.get("checksum").toString()) &&
                bitstream.getChecksumAlgorithm().equals(checksumMap.get("checksum_algorithm").toString());
    }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.BitstreamChecksum;
import org.dspace.app.rest.model.BitstreamChecksumRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.SyncBitstreamStorageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "checksum" subresource of an individual bitstream.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.NAME + "." + BitstreamRest.CHECKSUM)
public class BitstreamCheckSumLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    SyncBitstreamStorageServiceImpl syncBitstreamStorageService;

    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'READ')")
    public BitstreamChecksumRest getChecksum(@Nullable HttpServletRequest request,
                                      UUID bitstreamId,
                                      @Nullable Pageable optionalPageable,
                                      Projection projection) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            }

            CheckSumRest activeStoreChecksum = new CheckSumRest();
            CheckSumRest databaseChecksum = new CheckSumRest();
            CheckSumRest synchronizedStoreChecksum = new CheckSumRest();

            // Get the checksum from the active store
            composeChecksumRest(activeStoreChecksum, syncBitstreamStorageService.computeChecksum(context, bitstream));
            // Get the checksum from the database
            databaseChecksum.setCheckSumAlgorithm(bitstream.getChecksumAlgorithm());
            databaseChecksum.setValue(bitstream.getChecksum());

            if (syncBitstreamStorageService.isBitstreamStoreSynchronized(bitstream)) {
                // Get the checksum from the synchronized store
                composeChecksumRest(synchronizedStoreChecksum,
                        syncBitstreamStorageService.computeChecksumSpecStore(context, bitstream,
                                syncBitstreamStorageService.getSynchronizedStoreNumber(bitstream)));
            }

            BitstreamChecksum bitstreamChecksum = new BitstreamChecksum();
            bitstreamChecksum.setActiveStore(activeStoreChecksum);
            bitstreamChecksum.setDatabaseChecksum(databaseChecksum);
            bitstreamChecksum.setSynchronizedStore(synchronizedStoreChecksum);

            return converter.toRest(bitstreamChecksum, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compose the checksum rest object from the checksum map
     */
    private void composeChecksumRest(CheckSumRest checksumRest, Map<String, Object> checksumMap) {
        if (Objects.isNull(checksumMap)) {
            return;
        }
        if (checksumMap.containsKey("checksum")) {
            checksumRest.setValue(checksumMap.get("checksum").toString());
        }

        if (checksumMap.containsKey("checksum_algorithm")) {
            checksumRest.setCheckSumAlgorithm(checksumMap.get("checksum_algorithm").toString());
        }
    }
}

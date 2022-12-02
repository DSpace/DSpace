/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service.clarin;

import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Service interface class for the Item object.
 * This service is enhancement of the ItemService service for Clarin project purposes.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinItemService {

    /**
     * Find Item by the BitstreamUUID
     * @param context DSpace context object
     * @param bitstreamUUID UUID of the finding bitstream
     * @return found bitstream or null
     * @throws SQLException database error
     */
    List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException;
}

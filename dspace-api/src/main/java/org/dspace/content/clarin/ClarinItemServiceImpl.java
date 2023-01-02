/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.dao.clarin.ClarinItemDAO;
import org.dspace.content.service.clarin.ClarinItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Item object.
 * This service is enhancement of the ItemService service for Clarin project purposes.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinItemServiceImpl implements ClarinItemService {

    @Autowired
    ClarinItemDAO clarinItemDAO;

    @Override
    public List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException {
        return clarinItemDAO.findByBitstreamUUID(context, bitstreamUUID);
    }
}

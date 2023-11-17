/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.ItemRequests;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.LDNItemRequestsRest;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Rest Repository for LDN requests targeting items
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
@Component(LDNItemRequestsRest.CATEGORY + "." + LDNItemRequestsRest.NAME)
public class LDNItemRequestsRestRepository extends DSpaceRestRepository<LDNItemRequestsRest, String> {

    private static final Logger log = LogManager.getLogger(LDNItemRequestsRestRepository.class);

    @Autowired
    private LDNMessageService ldnMessageService;

    @SearchRestMethod(name = LDNItemRequestsRest.GET_ITEM_REQUESTS)
    //@PreAuthorize("hasAuthority('AUTHENTICATED')")
    public LDNItemRequestsRest findItemRequests(
        @Parameter(value = "itemuuid", required = true) UUID itemUuid) {

        log.info("START findItemRequests looking for requests for item " + itemUuid);
        Context context = obtainContext();
        ItemRequests resultRequests = new ItemRequests();
        try {
            resultRequests = ldnMessageService.findRequestsByItemUUID(context, itemUuid);
        } catch (SQLException e) {
            log.error(e);
        }
        log.info("END findItemRequests");
        return converter.toRest(resultRequests, utils.obtainProjection());
    }

    @Override
    public LDNItemRequestsRest findOne(Context context, String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<LDNItemRequestsRest> findAll(Context context, Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<LDNItemRequestsRest> getDomainClass() {
        // TODO Auto-generated method stub
        return null;
    }
}

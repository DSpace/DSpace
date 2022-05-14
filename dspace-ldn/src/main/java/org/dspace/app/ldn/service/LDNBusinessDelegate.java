/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Linked Data Notification business delegate to facilitate sending
 * notification.
 * 
 * @author Stefano Maffei (steph-ieffam @ 4Science)
 * @author William Welling
 * 
 */
public class LDNBusinessDelegate {

    @Autowired
    private BusinessLookUp businessLookUp;

    /**
     * @param  serviceName the service name to use
     * @param  ctx         the dspace context
     * @param  item        the dspace item
     * @throws Exception
     */
    public void handleRequest(String serviceName, Context ctx, Item item) throws Exception {
        BusinessService businessService = businessLookUp.getBusinessService(serviceName);

        businessService.doProcessing(ctx, item);
    }
}

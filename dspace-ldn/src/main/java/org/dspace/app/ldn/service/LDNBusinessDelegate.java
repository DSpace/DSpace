/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import org.dspace.content.Item;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Linked Data Notification business delegate to facilitate sending
 * notification.
 */
public class LDNBusinessDelegate {

    @Autowired
    private BusinessLookUp businessLookUp;

    /**
     * @param serviceName the service name to use
     * @param item the dspace item
     * @throws Exception
     */
    public void handleRequest(String serviceName, Item item) throws Exception{
        BusinessService businessService = businessLookUp.getBusinessService(serviceName);
        
        businessService.doProcessing(item); 
    }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import org.dspace.content.Item;

/**
 * The BusinessService interface defines common methods
 * used by the LDN services
 * 
 * @author Stefano Maffei (steph-ieffam @ 4Science)
 *
 */
interface BusinessService {

    /**
     * The Service logic
     * 
     * @param item the dspace item
     * @throws Exception in any error occurs
     */
    public void doProcessing(Item item) throws Exception;

    /**
     * Returns the service name that identifies the service
     * 
     * @return String
     */
    public String getServiceName();
}

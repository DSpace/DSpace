/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service;

import eu.dnetlib.broker.BrokerClient;
import org.dspace.utils.DSpace;

/**
 * Factory for the {@link BrokerClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OpenaireClientFactory {

    /**
     * Returns an instance of the {@link BrokerClient}.
     *
     * @return the client instance
     */
    public BrokerClient getBrokerClient();

    public static OpenaireClientFactory getInstance() {
        return new DSpace().getServiceManager().getServiceByName("openaireClientFactory", OpenaireClientFactory.class);
    }
}

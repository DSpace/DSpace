/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.impl;

import eu.dnetlib.broker.BrokerClient;
import org.dspace.qaevent.service.OpenaireClientFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OpenaireClientFactory} that returns the instance of
 * {@link BrokerClient} managed by the Spring context.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OpenaireClientFactoryImpl implements OpenaireClientFactory {

    @Autowired
    private BrokerClient brokerClient;

    @Override
    public BrokerClient getBrokerClient() {
        return brokerClient;
    }

    public void setBrokerClient(BrokerClient brokerClient) {
        this.brokerClient = brokerClient;
    }

}

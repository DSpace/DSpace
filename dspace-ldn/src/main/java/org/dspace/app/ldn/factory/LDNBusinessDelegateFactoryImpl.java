/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.service.LDNBusinessDelegate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Business delegate factory implementation that autowires business delegate for
 * static retrieval.
 */
public class LDNBusinessDelegateFactoryImpl extends LDNBusinessDelegateFactory {

    @Autowired(required = true)
    private LDNBusinessDelegate ldnBusinessDelegate;

    /**
     * @return LDNBusinessDelegate
     */
    @Override
    public LDNBusinessDelegate getLDNBusinessDelegate() {
        return ldnBusinessDelegate;
    }

}

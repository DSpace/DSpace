/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.factory;

import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the identifier package, use IdentifierServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class IdentifierServiceFactoryImpl extends IdentifierServiceFactory {

    @Autowired(required = true)
    private IdentifierService identifierService;
    @Autowired(required = true)
    private DOIService doiService;

    @Override
    public IdentifierService getIdentifierService() {
        return identifierService;
    }

    @Override
    public DOIService getDOIService() {
        return doiService;
    }
}

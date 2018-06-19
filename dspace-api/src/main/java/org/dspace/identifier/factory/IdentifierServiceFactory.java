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
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the identifier package, use IdentifierServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class IdentifierServiceFactory {

    public abstract IdentifierService getIdentifierService();

    public abstract DOIService getDOIService();

    public static IdentifierServiceFactory getInstance(){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("identifierServiceFactory", IdentifierServiceFactory.class);
    }
}

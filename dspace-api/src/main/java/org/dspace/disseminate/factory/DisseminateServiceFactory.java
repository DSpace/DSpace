/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate.factory;

import org.dspace.disseminate.service.CitationDocumentService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the disseminate package, use DisseminateServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class DisseminateServiceFactory {

    public abstract CitationDocumentService getCitationDocumentService();

    public static DisseminateServiceFactory getInstance()
    {
        return new DSpace().getServiceManager().getServiceByName("disseminateServiceFactory", DisseminateServiceFactory.class);
    }
}

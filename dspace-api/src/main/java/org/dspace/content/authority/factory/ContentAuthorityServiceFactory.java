/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.factory;

import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the content.authority package, use ContentAuthorityServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class ContentAuthorityServiceFactory {

    public abstract ChoiceAuthorityService getChoiceAuthorityService();

    public abstract MetadataAuthorityService getMetadataAuthorityService();

    public static ContentAuthorityServiceFactory getInstance(){
        return new DSpace().getServiceManager().getServiceByName("contentAuthorityServiceFactory", ContentAuthorityServiceFactory.class);
    }
}

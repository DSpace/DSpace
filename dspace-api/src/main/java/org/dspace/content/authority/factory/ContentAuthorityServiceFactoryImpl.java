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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the content.authority package, use ContentAuthorityServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class ContentAuthorityServiceFactoryImpl extends ContentAuthorityServiceFactory {

    @Autowired(required = true)
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired(required = true)
    private MetadataAuthorityService metadataAuthorityService;


    @Override
    public ChoiceAuthorityService getChoiceAuthorityService()
    {
        return choiceAuthorityService;
    }

    @Override
    public MetadataAuthorityService getMetadataAuthorityService()
    {
        return metadataAuthorityService;
    }
}

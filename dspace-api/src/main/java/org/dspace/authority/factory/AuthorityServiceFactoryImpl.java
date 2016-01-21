/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.factory;

import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthorityTypes;
import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.authority.indexer.AuthorityIndexingService;
import org.dspace.authority.service.AuthorityService;
import org.dspace.authority.service.AuthorityValueService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Factory implementation to get services for the authority package, use AuthorityServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class AuthorityServiceFactoryImpl extends AuthorityServiceFactory {

    @Autowired(required = true)
    private AuthorityValueService authorityValueService;

    @Autowired(required = true)
    private AuthorityTypes authorityTypes;

    @Autowired(required = true)
    private AuthorityService authorityService;

    @Autowired(required = true)
    private AuthorityIndexingService authorityIndexingService;

    @Autowired(required = true)
    private AuthoritySearchService authoritySearchService;

    @Autowired(required = true)
    private List<AuthorityIndexerInterface> authorityIndexerInterfaces;

    @Override
    public AuthorityValueService getAuthorityValueService() {
        return authorityValueService;
    }

    @Override
    public AuthorityTypes getAuthorTypes() {
        return authorityTypes;
    }

    @Override
    public AuthorityIndexingService getAuthorityIndexingService() {
        return authorityIndexingService;
    }

    @Override
    public AuthoritySearchService getAuthoritySearchService() {
        return authoritySearchService;
    }

    @Override
    public AuthorityService getAuthorityService() {
        return authorityService;
    }

    @Override
    public List<AuthorityIndexerInterface> getAuthorityIndexers() {
        return authorityIndexerInterfaces;
    }
}

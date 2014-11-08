/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license.factory;

import org.dspace.license.service.CreativeCommonsService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the license package, use LicenseServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class LicenseServiceFactoryImpl extends LicenseServiceFactory {

    @Autowired(required = true)
    private CreativeCommonsService creativeCommonsService;

    @Override
    public CreativeCommonsService getCreativeCommonsService() {
        return creativeCommonsService;
    }
}

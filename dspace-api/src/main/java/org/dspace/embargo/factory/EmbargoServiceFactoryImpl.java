/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo.factory;

import org.dspace.embargo.service.EmbargoService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the embargo package, use EmbargoServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class EmbargoServiceFactoryImpl extends EmbargoServiceFactory {

    @Autowired(required = true)
    private EmbargoService embargoService;

    @Override
    public EmbargoService getEmbargoService() {
        return embargoService;
    }
}

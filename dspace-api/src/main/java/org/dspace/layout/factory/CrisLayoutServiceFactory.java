/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.factory;

import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory for layout services.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public abstract class CrisLayoutServiceFactory {

    public static CrisLayoutServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("crisLayoutServiceFactory", CrisLayoutServiceFactory.class);
    }

    public abstract CrisLayoutTabService getTabService();

    public abstract CrisLayoutBoxService getBoxService();

    public abstract CrisLayoutFieldService getFieldService();

}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport.factory;

import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the itemimport package, use ItemImportService.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class ItemImportServiceFactory {

    public abstract ItemImportService getItemImportService();

    public static ItemImportServiceFactory getInstance(){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("itemImportServiceFactory", ItemImportServiceFactory.class);
    }
}

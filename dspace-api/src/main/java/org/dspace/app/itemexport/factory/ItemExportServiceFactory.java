/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport.factory;

import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the itemexport package, use ItemExportServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class ItemExportServiceFactory {

    public abstract ItemExportService getItemExportService();

    public static ItemExportServiceFactory getInstance(){
        return new DSpace().getServiceManager().getServiceByName("itemExportServiceFactory", ItemExportServiceFactory.class);
    }
}

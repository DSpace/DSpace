/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport.factory;

import org.dspace.app.itemexport.service.ItemExportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the itemexport package, use ItemExportServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class ItemExportServiceFactoryImpl extends ItemExportServiceFactory {

    @Autowired(required = true)
    private ItemExportService itemExportService;

    @Override
    public ItemExportService getItemExportService() {
        return itemExportService;
    }
}

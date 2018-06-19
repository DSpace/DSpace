/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport.factory;

import org.dspace.app.itemimport.service.ItemImportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the itemimport package, use ItemImportService.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class ItemImportServiceFactoryImpl extends ItemImportServiceFactory {

    @Autowired(required = true)
    private ItemImportService itemImportService;

    @Override
    public ItemImportService getItemImportService() {
        return itemImportService;
    }
}

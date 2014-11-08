/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore.factory;

import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the storage package, use StorageServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class StorageServiceFactory {

    public abstract BitstreamStorageService getBitstreamStorageService();

    public static StorageServiceFactory getInstance()
    {
        return new DSpace().getServiceManager().getServiceByName("storageServiceFactory", StorageServiceFactory.class);
    }
}

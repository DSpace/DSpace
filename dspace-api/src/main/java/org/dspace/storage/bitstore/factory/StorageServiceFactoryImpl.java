/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore.factory;

import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the storage package, use StorageServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class StorageServiceFactoryImpl extends StorageServiceFactory {

    @Autowired(required = true)
    private BitstreamStorageService bitstreamStorageService;

    @Override
    public BitstreamStorageService getBitstreamStorageService() {
        return bitstreamStorageService;
    }
}

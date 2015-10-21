/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess.factory;

import org.dspace.fileaccess.service.FileAccessFromMetadataService;
import org.dspace.fileaccess.service.ItemMetadataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 12/11/15
 * Time: 13:34
 */
public class FileAccessServiceFactoryImpl extends FileAccessServiceFactory {
    @Autowired(required = true)
    private FileAccessFromMetadataService fileAccessFromMetadataService;
    @Autowired(required = true)
    private ItemMetadataService itemMetadataService;

    @Override
    public FileAccessFromMetadataService getFileAccessFromMetadataService() {
        return fileAccessFromMetadataService;
    }

    @Override
    public ItemMetadataService getItemMetadataService() {
        return itemMetadataService;
    }
}

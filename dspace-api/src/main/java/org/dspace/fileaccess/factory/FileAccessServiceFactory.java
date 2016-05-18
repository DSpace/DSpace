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
import org.dspace.utils.DSpace;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 12/11/15
 * Time: 13:23
 */
public abstract class FileAccessServiceFactory {

    public abstract FileAccessFromMetadataService getFileAccessFromMetadataService();

    public abstract ItemMetadataService getItemMetadataService();

    public static FileAccessServiceFactory getInstance(){
        return new DSpace().getServiceManager().getServiceByName("fileAccessServiceFactory", FileAccessServiceFactory.class);
    }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess;

import org.dspace.content.*;
import org.dspace.fileaccess.service.*;

/**
 * This stub can be used to replace the Elsevier specific ElsevierItemMetadataServiceImpl.
 * comment out the ElsevierItemMetadataServiceImpl bean in config/spring/api/core-services.xml to disable it
 * uncomment the ItemMetadataServiceImpl bean in the same spring file to enable it.
 *
 * @author Created by Philip Vissenaekens (philip at atmire dot com)
 */
public class ItemMetadataServiceImpl implements ItemMetadataService {
    @Override
    public String getPII(Item item) {
        return null;
    }

    @Override
    public String getDOI(Item item) {
        return null;
    }

    @Override
    public String getEID(Item item) {
        return null;
    }

    @Override
    public String getScopusID(Item item) {
        return null;
    }

    @Override
    public String getPubmedID(Item item) {
        return null;
    }
}

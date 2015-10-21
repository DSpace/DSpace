/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess;

import org.dspace.content.Item;
import org.dspace.fileaccess.service.ItemMetadataService;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 12/11/15
 * Time: 13:51
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
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess.service;

import org.dspace.content.Item;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 26/10/15
 * Time: 13:17
 */
public interface ItemMetadataService {

    public String getPII(Item item);

    public String getDOI(Item item);
}

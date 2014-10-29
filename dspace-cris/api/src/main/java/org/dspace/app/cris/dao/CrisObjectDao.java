/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.Date;

import it.cilea.osd.common.dao.PaginableObjectDao;

import org.dspace.app.cris.model.ACrisObject;

public interface CrisObjectDao<T extends ACrisObject> extends PaginableObjectDao<T, Integer>
{

    T uniqueByCrisID(String crisID);

    T uniqueByID(Integer rp);

    T uniqueBySourceID(String sourceRef, String sourceID);

    T uniqueByUUID(String uuid);
    
    Date uniqueLastModifiedTimeStamp(Integer id);
    
}

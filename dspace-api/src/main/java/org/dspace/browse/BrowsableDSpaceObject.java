/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import org.dspace.core.Context;

public interface BrowsableDSpaceObject<PK extends Serializable> {

    public Map<String, Object> getExtraInfo();

    public boolean isArchived();

    public boolean isDiscoverable();

    public String getName();

    public String findHandle(Context context) throws SQLException;

    public boolean haveHierarchy();

    public BrowsableDSpaceObject<PK> getParentObject();

    public Date getLastModified();

    public int getType();

    public PK getID();

    default String getUniqueIndexID() {
        return getType() + "-" + getID().toString();
    }

    public String getHandle();

    public String getTypeText();
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface Version
{
    public EPerson getEperson();
    public int getItemID();
    public Date getVersionDate();
    public int getVersionNumber();
    public String getSummary();
    public int getVersionHistoryID();
    public int getVersionId();
    public Item getItem();
}


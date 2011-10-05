package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 30, 2011
 * Time: 1:45:36 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Version {
    public EPerson getEperson();
    public int getItemID();
    public Date getVersionDate();
    public int getVersionNumber();
    public String getSummary();
    public int getVersionHistoryID();
    public int getVersionId();
    public Item getItem();   
}


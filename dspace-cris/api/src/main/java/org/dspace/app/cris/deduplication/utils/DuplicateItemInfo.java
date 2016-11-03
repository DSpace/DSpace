/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.util.ActionUtils;

public class DuplicateItemInfo
{

    private int dedupID = -1;

    private SimpleViewEntityDTO duplicateItem;

    private String note;

    private EPerson eperson;

    private boolean notDuplicate;

    private boolean rejected;

    private EPerson reader;

    private boolean toFix;

    private Date rejectDate;

    private Date readDate;

    private EPerson admin;

    private Date adminDate;

    public int getDedupID()
    {
        return dedupID;
    }

    public void setDedupID(int dedupID)
    {
        this.dedupID = dedupID;
    }

    public SimpleViewEntityDTO getDuplicateItem()
    {
        return duplicateItem;
    }

    public void setDuplicateItem(SimpleViewEntityDTO duplicateItem)
    {
        this.duplicateItem = duplicateItem;
    }

    public EPerson getEperson()
    {
        return eperson;
    }

    public void setEperson(EPerson eperson)
    {
        this.eperson = eperson;
    }

    public void setRejected(boolean rejected)
    {
        this.rejected = rejected;
    }

    public boolean isRejected()
    {
        return rejected;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public boolean isNotDuplicate()
    {
        return notDuplicate;
    }

    public void setNotDuplicate(boolean notDuplicate)
    {
        this.notDuplicate = notDuplicate;
    }

    public EPerson getAdmin()
    {
        return admin;
    }

    public EPerson getReader()
    {
        return reader;
    }

    public void setReader(EPerson reader)
    {
        this.reader = reader;
    }

    public boolean isToFix()
    {
        return toFix;
    }

    public void setToFix(boolean toFix)
    {
        this.toFix = toFix;
    }

    public Date getRejectDate()
    {
        return rejectDate;
    }

    public void setRejectDate(Date rejectDate)
    {
        this.rejectDate = rejectDate;
    }

    public Date getReadDate()
    {
        return readDate;
    }

    public void setReadDate(Date readDate)
    {
        this.readDate = readDate;
    }

    public Date getAdminDate()
    {
        return adminDate;
    }

    public void setAdminDate(Date adminDate)
    {
        this.adminDate = adminDate;
    }

    public void setAdmin(EPerson admin)
    {
        this.admin = admin;
    }

    protected void setDefaultActions(Context context, Boolean check)
            throws SQLException
    {
        List<String> actions = new ArrayList<String>();
        if (check != null)
        {
            if (check)
            {
                actions.add(ActionUtils.ACTION_FAKE_WF2);
                actions.add(ActionUtils.ACTION_IGNORE_WF2);
            }
            else
            {
                actions.add(ActionUtils.ACTION_IGNORE_WS);
                actions.add(ActionUtils.ACTION_FAKE_WS);
            }
        }
        duplicateItem.setActions(actions);
    }
    
    protected void setCustomActions(Context context)
            throws SQLException
    {
        List<String> actions = new ArrayList<String>();
        actions.add(ActionUtils.ACTION_FAKE_WF1);
        actions.add(ActionUtils.ACTION_IGNORE_WF1);
        duplicateItem.setActions(actions);
    }
}

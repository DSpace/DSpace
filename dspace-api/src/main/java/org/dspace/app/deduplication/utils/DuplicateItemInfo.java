/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.model.DuplicateDecisionValue;
import org.dspace.content.DSpaceObject;

public class DuplicateItemInfo {

    private int dedupID = -1;

    private /* BrowsableDSpaceObject */DSpaceObject duplicateItem;

    private int duplicateItemType;

    private Map<DuplicateDecisionType, String> notes = new HashMap<DuplicateDecisionType, String>();
    // private EPerson eperson;
    //
    // private boolean notDuplicate;
    //
    // private boolean rejected;
    //
    // private EPerson reader;
    //
    // private boolean toFix;
    //
    // private Date rejectDate;
    //
    // private Date readDate;
    //
    // private EPerson admin;
    //
    // private Date adminDate;

    private Map<DuplicateDecisionType, DuplicateDecisionValue> decisions =
            new HashMap<DuplicateDecisionType, DuplicateDecisionValue>();

    public int getDedupID() {
        return dedupID;
    }

    public void setDedupID(int dedupID) {
        this.dedupID = dedupID;
    }

    public /* BrowsableDSpaceObject */DSpaceObject getDuplicateItem() {
        return duplicateItem;
    }

    public void setDuplicateItem(/* BrowsableDSpaceObject */DSpaceObject duplicateItem) {
        this.duplicateItem = duplicateItem;
    }

    public int getDuplicateItemType() {
        return duplicateItemType;
    }

    public void setDuplicateItemType(int duplicateItemType) {
        this.duplicateItemType = duplicateItemType;
    }

    // public EPerson getEperson()
    // {
    // return eperson;
    // }
    //
    // public void setEperson(EPerson eperson)
    // {
    // this.eperson = eperson;
    // }
    //
    // public void setRejected(boolean rejected)
    // {
    // this.rejected = rejected;
    // }
    //
    // public boolean isRejected()
    // {
    // return rejected;
    // }

    // public boolean isNotDuplicate()
    // {
    // return notDuplicate;
    // }
    //
    // public void setNotDuplicate(boolean notDuplicate)
    // {
    // this.notDuplicate = notDuplicate;
    // }
    //
    // public EPerson getAdmin()
    // {
    // return admin;
    // }
    //
    // public EPerson getReader()
    // {
    // return reader;
    // }
    //
    // public void setReader(EPerson reader)
    // {
    // this.reader = reader;
    // }
    //
    // public boolean isToFix()
    // {
    // return toFix;
    // }
    //
    // public void setToFix(boolean toFix)
    // {
    // this.toFix = toFix;
    // }
    //
    // public Date getRejectDate()
    // {
    // return rejectDate;
    // }
    //
    // public void setRejectDate(Date rejectDate)
    // {
    // this.rejectDate = rejectDate;
    // }
    //
    // public Date getReadDate()
    // {
    // return readDate;
    // }
    //
    // public void setReadDate(Date readDate)
    // {
    // this.readDate = readDate;
    // }
    //
    // public Date getAdminDate()
    // {
    // return adminDate;
    // }
    //
    // public void setAdminDate(Date adminDate)
    // {
    // this.adminDate = adminDate;
    // }
    //
    // public void setAdmin(EPerson admin)
    // {
    // this.admin = admin;
    // }

    public DuplicateDecisionValue getDecision(DuplicateDecisionType type) {
        return decisions.get(type);
    }

    public void setDecision(DuplicateDecisionType type, DuplicateDecisionValue decision) {
        this.decisions.put(type, decision);
    }

    public String getNote(DuplicateDecisionType type) {
        return notes.get(type);
    }

    public void setNote(DuplicateDecisionType type, String note) {
        this.notes.put(type, note);
    }
}

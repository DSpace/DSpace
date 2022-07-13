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

    private DSpaceObject duplicateItem;

    private int duplicateItemType;

    private Map<DuplicateDecisionType, String> notes = new HashMap<DuplicateDecisionType, String>();

    private Map<DuplicateDecisionType, DuplicateDecisionValue> decisions =
            new HashMap<DuplicateDecisionType, DuplicateDecisionValue>();

    public int getDedupID() {
        return dedupID;
    }

    public void setDedupID(int dedupID) {
        this.dedupID = dedupID;
    }

    public DSpaceObject getDuplicateItem() {
        return duplicateItem;
    }

    public void setDuplicateItem(DSpaceObject duplicateItem) {
        this.duplicateItem = duplicateItem;
    }

    public int getDuplicateItemType() {
        return duplicateItemType;
    }

    public void setDuplicateItemType(int duplicateItemType) {
        this.duplicateItemType = duplicateItemType;
    }

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

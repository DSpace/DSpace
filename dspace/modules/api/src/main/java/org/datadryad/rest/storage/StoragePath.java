/*
 */
package org.datadryad.rest.storage;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Journal;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class StoragePath extends ArrayList<StoragePathElement> {
    public void addPathElement(String key, String value) {
        this.add(new StoragePathElement(key,value));
    }

    public List<String> getKeyPath() {
        ArrayList<String> keyPath = new ArrayList<String>();
        for(StoragePathElement element : this) {
            keyPath.add(element.key);
        }
        return keyPath;
    }

    public List<String> getValuePath() {
        ArrayList<String> valuePath = new ArrayList<String>();
        for(StoragePathElement element : this) {
            valuePath.add(element.value);
        }
        return valuePath;
    }

    public Boolean validElements() {
        for(String value : getValuePath()) {
            if(value.length() == 0) {
                return false;
            }
        }
        return true;
    }

    // Methods for accessing/creating StoragePaths for Manuscripts and Organizations

    public static StoragePath createJournalPath(String journalCode) {
        StoragePath path = new StoragePath();
        path.addPathElement(Journal.JOURNAL_CODE, journalCode);
        return path;
    }

    public static StoragePath createManuscriptPath(String journalCode, String manuscriptId) {
        StoragePath path = new StoragePath();
        path.addPathElement(Journal.JOURNAL_CODE, journalCode);
        path.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
        return path;
    }

    public void setJournalCode(String journalCode) {
        if (getJournalCode() == null) {   // can't add ManuscriptId to a path that doesn't have an journal
            this.addPathElement(Journal.JOURNAL_CODE, journalCode);
        } else {
            this.set(0, new StoragePathElement(Journal.JOURNAL_CODE, journalCode));
        }
    }

    public void setManuscriptId(String manuscriptId) {
        if (this.getJournalCode() == null) {   // can't add ManuscriptId to a path that doesn't have an journal
            return;
        }
        if (this.getManuscriptId() == null) {
            this.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
        } else {
            this.set(1, new StoragePathElement(Manuscript.MANUSCRIPT_ID, manuscriptId));
        }
    }

    public String getJournalCode() {
        if(this.size() >= 1) {
            String journalCode = this.get(0).value;
            return journalCode;
        } else {
            return null;
        }
    }

    public String getManuscriptId() {
        if(this.size() >= 2) {
            String manuscriptId = this.get(1).value;
            return manuscriptId;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "/" + StringUtils.join(getValuePath().toArray(), "/");
    }
}

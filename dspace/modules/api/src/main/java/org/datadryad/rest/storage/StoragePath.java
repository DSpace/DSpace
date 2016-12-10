/*
 */
package org.datadryad.rest.storage;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class StoragePath extends ArrayList<StoragePathElement> {
    public static final String JOURNAL_PATH = "journalRef";
    public static final String MANUSCRIPT_PATH = "manuscript";

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

    // Methods for accessing/creating StoragePaths for Manuscripts and Journals

    public static StoragePath createJournalPath(String journalRef) {
        StoragePath path = new StoragePath();
        path.addPathElement(StoragePath.JOURNAL_PATH, journalRef);
        return path;
    }

    public static StoragePath createManuscriptPath(String journalRef, String manuscriptId) {
        StoragePath path = new StoragePath();
        path.addPathElement(StoragePath.JOURNAL_PATH, journalRef);
        path.addPathElement(StoragePath.MANUSCRIPT_PATH, manuscriptId);
        return path;
    }

    public static StoragePath createPackagesPath(String journalRef) {
        return createManuscriptPath(journalRef, "packages");
    }

    public void setJournalCode(String journalRef) {
        if (getJournalRef() == null) {   // can't add ManuscriptId to a path that doesn't have an journal
            this.addPathElement(StoragePath.JOURNAL_PATH, journalRef);
        } else {
            this.set(0, new StoragePathElement(StoragePath.JOURNAL_PATH, journalRef));
        }
    }

    public void setManuscriptId(String manuscriptId) {
        if (this.getJournalRef() == null) {   // can't add ManuscriptId to a path that doesn't have an journal
            return;
        }
        if (this.getManuscriptId() == null) {
            this.addPathElement(StoragePath.MANUSCRIPT_PATH, manuscriptId);
        } else {
            this.set(1, new StoragePathElement(StoragePath.MANUSCRIPT_PATH, manuscriptId));
        }
    }

    public String getJournalRef() {
        if(this.size() >= 1) {
            return this.get(0).value;
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

/*
 */
package org.datadryad.rest.storage;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;

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

    public static StoragePath createOrganizationPath(String organizationCode) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        return path;
    }

    public static StoragePath createManuscriptPath(String organizationCode, String manuscriptId) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        path.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
        return path;
    }

    public void setOrganizationCode(String organizationCode) {
        if (getOrganizationCode() == null) {   // can't add ManuscriptId to a path that doesn't have an organization
            this.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        } else {
            this.set(0, new StoragePathElement(Organization.ORGANIZATION_CODE, organizationCode));
        }
    }

    public void setManuscriptId(String manuscriptId) {
        if (this.getOrganizationCode() == null) {   // can't add ManuscriptId to a path that doesn't have an organization
            return;
        }
        if (this.getManuscriptId() == null) {
            this.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
        } else {
            this.set(1, new StoragePathElement(Manuscript.MANUSCRIPT_ID, manuscriptId));
        }
    }

    public String getOrganizationCode() {
        if(this.size() >= 1) {
            String organizationCode = this.get(0).value;
            return organizationCode;
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

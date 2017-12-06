/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

/**
 * http://support.orcid.org/knowledgebase/articles/118807
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkExternalIdentifier {

    private WorkExternalIdentifierType workExternalIdentifierType;
    private String workExternalIdenfitierID;

    public WorkExternalIdentifier(WorkExternalIdentifierType workExternalIdentifierType, String workExternalIdenfitierID) {
        this.workExternalIdentifierType = workExternalIdentifierType;
        this.workExternalIdenfitierID = workExternalIdenfitierID;
    }

    public WorkExternalIdentifierType getWorkExternalIdentifierType() {
        return workExternalIdentifierType;
    }

    public void setWorkExternalIdentifierType(WorkExternalIdentifierType workExternalIdentifierType) {
        this.workExternalIdentifierType = workExternalIdentifierType;
    }

    @Override
    public String toString() {
        return "WorkExternalIdentifier{" +
                "workExternalIdentifierType=" + workExternalIdentifierType +
                ", workExternalIdenfitierID='" + workExternalIdenfitierID + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkExternalIdentifier that = (WorkExternalIdentifier) o;

        if (workExternalIdenfitierID != null ? !workExternalIdenfitierID.equals(that.workExternalIdenfitierID) : that.workExternalIdenfitierID != null) {
            return false;
        }
        if (workExternalIdentifierType != that.workExternalIdentifierType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = workExternalIdentifierType != null ? workExternalIdentifierType.hashCode() : 0;
        result = 31 * result + (workExternalIdenfitierID != null ? workExternalIdenfitierID.hashCode() : 0);
        return result;
    }
}

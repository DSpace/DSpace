/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class BioName {

    protected String givenNames;
    protected String familyName;
    protected String creditName;
    protected List<String> otherNames;

    BioName() {
        otherNames = new ArrayList<String>();
    }

    BioName(String givenNames, String familyName, String creditName, List<String> otherNames) {
        this.givenNames = givenNames;
        this.familyName = familyName;
        this.creditName = creditName;
        this.otherNames = otherNames;
    }

    public String getGivenNames() {
        return givenNames;
    }

    public void setGivenNames(String givenNames) {
        this.givenNames = givenNames;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getCreditName() {
        return creditName;
    }

    public void setCreditName(String creditName) {
        this.creditName = creditName;
    }

    public List<String> getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(List<String> otherNames) {
        this.otherNames = otherNames;
    }

    @Override
    public String toString() {
        return "BioName{" +
                "givenNames='" + givenNames + '\'' +
                ", familyName='" + familyName + '\'' +
                ", creditName='" + creditName + '\'' +
                ", otherNames=" + otherNames +
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

        BioName bioName = (BioName) o;

        if (creditName != null ? !creditName.equals(bioName.creditName) : bioName.creditName != null) {
            return false;
        }
        if (familyName != null ? !familyName.equals(bioName.familyName) : bioName.familyName != null) {
            return false;
        }
        if (givenNames != null ? !givenNames.equals(bioName.givenNames) : bioName.givenNames != null) {
            return false;
        }
        if (otherNames != null ? !otherNames.equals(bioName.otherNames) : bioName.otherNames != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = givenNames != null ? givenNames.hashCode() : 0;
        result = 31 * result + (familyName != null ? familyName.hashCode() : 0);
        result = 31 * result + (creditName != null ? creditName.hashCode() : 0);
        result = 31 * result + (otherNames != null ? otherNames.hashCode() : 0);
        return result;
    }
}

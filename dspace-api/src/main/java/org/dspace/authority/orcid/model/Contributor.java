/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.orcid.model;

import java.util.Set;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Contributor {

    private String orcid;
    private String creditName;
    private String email;
    private Set<ContributorAttribute> contributorAttributes;

    public Contributor(String orcid, String creditName, String email, Set<ContributorAttribute> contributorAttributes) {
        this.orcid = orcid;
        this.creditName = creditName;
        this.email = email;
        this.contributorAttributes = contributorAttributes;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getCreditName() {
        return creditName;
    }

    public void setCreditName(String creditName) {
        this.creditName = creditName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<ContributorAttribute> getContributorAttributes() {
        return contributorAttributes;
    }

    public void setContributorAttributes(Set<ContributorAttribute> contributorAttributes) {
        this.contributorAttributes = contributorAttributes;
    }

    @Override
    public String toString() {
        return "Contributor{" +
                "orcid='" + orcid + '\'' +
                ", creditName='" + creditName + '\'' +
                ", email='" + email + '\'' +
                ", contributorAttributes=" + contributorAttributes +
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

        Contributor that = (Contributor) o;

        if (contributorAttributes != null ? !contributorAttributes.equals(that.contributorAttributes) : that.contributorAttributes != null) {
            return false;
        }
        if (creditName != null ? !creditName.equals(that.creditName) : that.creditName != null) {
            return false;
        }
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (orcid != null ? !orcid.equals(that.orcid) : that.orcid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = orcid != null ? orcid.hashCode() : 0;
        result = 31 * result + (creditName != null ? creditName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (contributorAttributes != null ? contributorAttributes.hashCode() : 0);
        return result;
    }
}

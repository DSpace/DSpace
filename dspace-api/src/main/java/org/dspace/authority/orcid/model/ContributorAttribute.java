/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.orcid.model;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ContributorAttribute {

    private ContributorAttributeRole role;
    private ContributorAttributeSequence sequence;

    public ContributorAttribute(ContributorAttributeRole role, ContributorAttributeSequence sequence) {
        this.role = role;
        this.sequence = sequence;
    }

    public ContributorAttributeRole getRole() {
        return role;
    }

    public void setRole(ContributorAttributeRole role) {
        this.role = role;
    }

    public ContributorAttributeSequence getSequence() {
        return sequence;
    }

    public void setSequence(ContributorAttributeSequence sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return "ContributorAttribute{" +
                "role=" + role +
                ", sequence=" + sequence +
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

        ContributorAttribute that = (ContributorAttribute) o;

        if (role != that.role) {
            return false;
        }
        if (sequence != that.sequence) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = role != null ? role.hashCode() : 0;
        result = 31 * result + (sequence != null ? sequence.hashCode() : 0);
        return result;
    }
}
